## Dataset generation tool

You can use this software to generate a new text dataset as a transformation of other input datasets.

The following transformations are supported:
- Sentence (re)ordering, which swaps randomly sentences in the text of each file separately.
- Sentence replacement, which randomly replaces a sentence with a random sentence from a random file. The latter comes from a special replacement source dataset.
- Merging, where half (sentence-wise) the text is replaced with the other half text from another random file from the input dataset.

You can specify many parameters for controlling the behaviour of the process.
See `skel/config.txt` for details.