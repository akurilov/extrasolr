package com.github.akurilov.extrasolr.parse;

import com.github.akurilov.extrasolr.mq.MessageHandler;
import com.github.akurilov.extrasolr.mq.MessageQueue;
import com.github.akurilov.extrasolr.mq.nats.NatsMessageQueue;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.function.Consumer;

import static com.github.akurilov.extrasolr.Config.SUBJECT_INDEX;
import static com.github.akurilov.extrasolr.Config.SUBJECT_PARSE;
import static com.github.akurilov.extrasolr.Config.QUEUE_HOSTS;
import static com.github.akurilov.extrasolr.Config.SUBJECT_FETCH;
import static java.nio.charset.StandardCharsets.UTF_8;

public class ParseService {

    private static final Logger LOG = LoggerFactory.getLogger(ParseService.class);

    public static void main(final String... args)
    throws Exception {
        try(final var mq = new NatsMessageQueue(QUEUE_HOSTS)) {
            final var linkUrlConsumer = (Consumer<String>) (url) ->
                mq.publish(SUBJECT_FETCH, null, url.getBytes(UTF_8));
            final var contentHandler = (MessageHandler) (srcUrl, content) ->
                onContent(linkUrlConsumer, mq, srcUrl, content);
            mq.subscribe(SUBJECT_PARSE, contentHandler);
            Thread.currentThread().join();
        }
    }

    static void onContent(
        final Consumer<String> linkUrlConsumer, final MessageQueue mq, final String srcUrl, final byte[] content
    ) {
        final var contentTxt = new String(content, UTF_8);
        final var htmlSrc = new Source(contentTxt);
        htmlSrc.fullSequentialParse();
        extractLinks(linkUrlConsumer, htmlSrc);
        extractText(mq, srcUrl, htmlSrc);
    }

    static void extractLinks(final Consumer<String> linkUrlConsumer, final Segment htmlSrc) {
        htmlSrc
            .getAllElements(HTMLElementName.A)
            .parallelStream()
            .map(ParseService::hrefAttributeValue)
            .forEach(linkUrlConsumer);
    }

    static void extractText(final MessageQueue mq, final String srcUrl, final Segment htmlSrc) {
        final var txtExtractor = htmlSrc.getTextExtractor();
        txtExtractor.setConvertNonBreakingSpaces(true);
        txtExtractor.setExcludeNonHTMLElements(true);
        txtExtractor.setIncludeAttributes(false);
        final var txt = txtExtractor.toString();
        mq.publish(SUBJECT_INDEX, srcUrl, txt.getBytes(UTF_8));
    }

    static String hrefAttributeValue(final Element e) {
        return e.getAttributeValue("href");
    }
}
