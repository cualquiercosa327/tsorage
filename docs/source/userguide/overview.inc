*************************
Overview and Generalities
*************************

Generalities
============


.. _UG_Duration_Literals:

Duration Literals
-----------------

A duration literal represents an interval of time, which must be positive. In a configuration file, a duration literal is represented by a string, the format of which is ``<length><unit>``, where whitespace is allowed before, between, and after the parts.
``length`` must be either a positive integer, or a positive floatting number. ``unit`` must be one of the following:

- ``d``, ``day`` or ``days``
- ``h``, ``hour`` or ``hours``
- ``min``, ``minute`` or ``minutes``
- ``s``, ``second`` or ``seconds``
- ``ms``, ``milli``, ``millisecond`` or ``milliseconds``