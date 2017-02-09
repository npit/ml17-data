## Dataset generation tool

You can use this software to generate a new text dataset as a combination and/or transformation of other input datasets.

The following transformations are supported:
- Sentence (re)ordering (SO), which swaps randomly sentences in the text of each file separately.
- Sentence replacement (SR), which randomly replaces a sentence with a random sentence from a random file. The latter comes from a special replacement source dataset.
- Merging (ME), where half the text (sentence-wise) is replaced with the other half text from another random file from the input dataset.

SR and ME is performed in a language-consistent way.

You can specify many parameters for controlling the behaviour of the process.
See `skel/config.txt` for details.
