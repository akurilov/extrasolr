package com.github.akurilov.extrasolr.parse;

import com.github.akurilov.extrasolr.mq.MessageHandler;
import com.github.akurilov.extrasolr.mq.MessageQueue;
import net.htmlparser.jericho.Element;
import net.htmlparser.jericho.HTMLElementName;
import net.htmlparser.jericho.Segment;
import net.htmlparser.jericho.Source;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Objects;
import java.util.concurrent.atomic.LongAdder;
import java.util.function.Predicate;

import static com.github.akurilov.extrasolr.Config.SUBJECT_FETCH;
import static com.github.akurilov.extrasolr.Config.SUBJECT_INDEX;
import static java.nio.charset.StandardCharsets.UTF_8;

final class ParseMessageHandler
implements MessageHandler {

    private static final Logger LOG = LoggerFactory.getLogger(ParseMessageHandler.class);
    private static final int UNIQUE_URLS_CACHE_LIMIT = 1_000_000;

    private final MessageQueue mq;
    private final LongAdder succCounter;
    private final LongAdder failCounter;
    private final Predicate<String> urlFilter;

    ParseMessageHandler(final MessageQueue mq, final LongAdder succCounter, final LongAdder failCounter) {
        this.mq = mq;
        this.succCounter = succCounter;
        this.failCounter = failCounter;
        this.urlFilter = new FixedCacheUniquenessFilter<>(UNIQUE_URLS_CACHE_LIMIT);
    }

    @Override
    public final void accept(final String srcUrl, final byte[] content) {
        final var contentTxt = new String(content, UTF_8);
        try {
            final var htmlSrc = new Source(contentTxt);
            htmlSrc.fullSequentialParse();
            extractLinks(htmlSrc);
            extractText(srcUrl, htmlSrc);
            succCounter.increment();
        } catch(final Exception e) {
            LOG.warn(srcUrl + ": failed to parse the content", e);
            failCounter.increment();
        }
    }

    void extractLinks(final Segment htmlSrc) {
        htmlSrc
            .getAllElements(HTMLElementName.A)
            .parallelStream()
            .map(ParseMessageHandler::hrefAttributeValue)
            .filter(Objects::nonNull)
            .filter(urlFilter)
            .forEach(this::handleLinkUrl);
    }

    void extractText(final String srcUrl, final Segment htmlSrc) {
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

    void handleLinkUrl(final String url) {
        mq.publish(SUBJECT_FETCH, null, url.getBytes(UTF_8));
    }
}
