package dev.morphia.test.mapping.codec;

import dev.morphia.mapping.codec.writer.DocumentWriter;
import dev.morphia.test.TestBase;
import org.bson.Document;
import org.json.JSONException;
import org.skyscreamer.jsonassert.JSONAssert;
import org.testng.Assert;
import org.testng.annotations.Test;

import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.array;
import static dev.morphia.aggregation.experimental.codecs.ExpressionHelper.document;
import static java.util.Arrays.asList;
import static java.util.List.of;

public class TestDocumentWriter extends TestBase {
    private int docs = 0;
    private int arrays = 0;

    @Test
    public void arrays() {
        DocumentWriter writer = new DocumentWriter(getMapper());

        document(writer, () -> {
            check(writer, 1, 0);
            array(writer, "stuff", () -> {
                check(writer, 1, 1);
                writer.writeString("hello");
                writer.writeInt32(42);
            });
            check(writer, 1, 0);
            writer.writeName("next");
            writer.writeString("something simple");
        });

        check(writer, 0, 0);
        Assert.assertEquals(writer.getDocument(), new Document("stuff", asList("hello", 42))
                                                      .append("next", "something simple"));
    }

    @Test
    public void arraysWithDocs() {
        DocumentWriter writer = new DocumentWriter(getMapper());

        document(writer, () -> {
            check(writer, 1, 0);
            array(writer, "stuff", () -> {
                check(writer, 1, 1);
                document(writer, () -> {
                    writer.writeInt32("doc", 42);
                });
            });
            check(writer, 1, 0);
        });

        check(writer, 0, 0);
        Assert.assertEquals(writer.getDocument(), new Document("stuff", asList(new Document("doc", 42))));
    }

    @Test
    public void basic() {
        for (int i = 0; i < 3; i++) {
            DocumentWriter writer = new DocumentWriter(getMapper());
            Document expected = new Document();

            int finalI = i;
            document(writer, () -> {
                for (int j = 0; j < finalI; j++) {
                    writer.writeInt32("entry " + j, j);
                    expected.put("entry " + j, j);
                }
            });
            check(writer, 0, 0);
            Assert.assertEquals(expected, writer.getDocument());
        }
    }

    @Test
    public void nestedArrays() {
        DocumentWriter writer = new DocumentWriter(getMapper());

        document(writer, () -> {
            array(writer, "top", () -> {
                array(writer, () -> {
                    writer.writeInt32(1);
                    writer.writeInt32(2);
                    writer.writeInt32(3);
                    document(writer, () -> {
                        writer.writeString("nested", "string");
                    });
                });
            });
        });
        Document top = new Document("top", of(of(1, 2, 3, new Document("nested", "string"))));
        Assert.assertEquals(top, writer.getDocument());
    }

    @Test
    public void nesting() throws JSONException {
        String expected = "{$group : {_id : {$dateToString: {format: \"%Y-%m-%d\", date: \"$date\"}}, totalSaleAmount: {$sum: "
                          + "{$multiply: [ \"$price\", \"$quantity\" ]}}, averageQuantity: {$avg: \"$quantity\"},count: {$sum: 1}}}";

        DocumentWriter writer = new DocumentWriter(getMapper());
        document(writer, () -> {
            document(writer, "$group", () -> {

                document(writer, "_id", () -> {
                    document(writer, "$dateToString", () -> {
                        writer.writeString("format", "%Y-%m-%d");
                        writer.writeString("date", "$date");
                    });
                });

                document(writer, "totalSaleAmount", () -> {
                    document(writer, "$sum", () -> {
                        array(writer, "$multiply", () -> {
                            writer.writeString("$price");
                            writer.writeString("$quantity");
                        });
                    });
                });

                document(writer, "averageQuantity", () -> writer.writeString("$avg", "$quantity"));

                document(writer, "count", () -> {
                    writer.writeInt32("$sum", 1);
                });

            });
        });
        String s = writer.getDocument().toJson();
        JSONAssert.assertEquals(expected, s, false);
    }

    @Test
    public void subdocuments() {
        DocumentWriter writer = new DocumentWriter(getMapper());
        document(writer, () -> {
            writer.writeName("subdoc");
            document(writer, () -> writer.writeInt32("nested", 42));
        });

        check(writer, 0, 0);
        Assert.assertEquals(writer.getDocument(), new Document("subdoc", new Document("nested", 42)));

    }

    private void check(DocumentWriter writer, int docs, int arrays) {
        Assert.assertEquals(docs, writer.getDocsLevel());
        Assert.assertEquals(arrays, writer.getArraysLevel());
    }

    private void endArray(DocumentWriter writer) {
        writer.writeEndArray();
        Assert.assertEquals(--arrays, writer.getArraysLevel());
    }

    private void endDoc(DocumentWriter writer) {
        writer.writeEndDocument();
        Assert.assertEquals(--docs, writer.getDocsLevel());
    }

    private void startArray(DocumentWriter writer) {
        writer.writeStartArray();
        Assert.assertEquals(++arrays, writer.getArraysLevel());
    }

    private void startArray(DocumentWriter writer, String name) {
        writer.writeStartArray(name);
        Assert.assertEquals(++arrays, writer.getArraysLevel());
    }

    private void startDoc(DocumentWriter writer) {
        writer.writeStartDocument();
        Assert.assertEquals(++docs, writer.getDocsLevel());
    }

}
