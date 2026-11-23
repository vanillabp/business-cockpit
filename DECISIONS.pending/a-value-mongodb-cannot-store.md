### Addendum to decision 38: a value MongoDB cannot store fails every time

This entry adds to decision 38. Its list of the failures which come back every time gets three more
kinds. The rest of decision 38 stays as it is.

#### What was found

Decision 38 counts a failure as one which comes back every time when it depends on nothing but the
document. Its list missed the failures of a value which cannot be turned into what MongoDB stores,
so such a report counted as a failure for now and was repeated.

#### The decision

These failures come back every time as well (`OutcomeOfStoring`):

- A `ConversionFailedException` of Spring. A converter refuses a value. The known case is a decimal
  number with more than the 34 digits MongoDB's `Decimal128` keeps, or with an exponent out of its
  range. A report over Kafka carries every number as a decimal, so `new BigDecimal(0.1)` is such a
  number.
- A `CodecConfigurationException` of the driver. The driver has no codec for the type of a value.
  The known case is a `BigInteger`, which Jackson makes of a whole number of a JSON body beyond the
  range of a `long`. A decimal number of a JSON body becomes a `double` and is stored rounded.
- A `BsonSerializationException` of the driver. It cannot encode the document.
  `BsonMaximumSizeExceededException`, which decision 38 lists already, is one of them.

Each of them is thrown before anything is sent. REST answers 422 and Kafka skips the record, as
decision 38 says for the other failures of this kind. The reason says that the record holds a value
MongoDB cannot store, and it does not repeat the value, because the message of a converter does.

#### What stays as it was

The cockpit does not round a number or turn a `BigInteger` into another type. Rounding would drop
digits a workflow module sent on purpose and helps for the digits only, not for the exponent. The
wiki page about reporting tells a workflow module to round such a value itself.
