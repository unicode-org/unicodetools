# Emoji Charts and Data

### Emoji beta charts

always check and post the files that have changed in:

<http://www.unicode.org/draft/emoji/>

*   \*.html
*   charts-BBBB/\* (except /internal/) - where BBBB is the beta release version
    — in this case 12.0
*   [future/](http://www.unicode.org/draft/emoji/future/)\* (except /internal/)
*   [images/](http://www.unicode.org/draft/emoji/images/)\*

In this case you should also post the following (just regenerated) since there
are new images, and some changes in layout.

*   charts-RRRR/\* (except /internal/) - where RRRR is the current release
    version — in this case 11.0

(linking checking to be done with the "flag" versions. please let me know of any
problems (though if there are no visible artifacts it isn't a showstopper))

First time posting a new set of beta charts, make sure that charts-beta is a
newly created symlink to charts-BBBB.

### Emoji beta data

Copy all files under draft/Public/emoji/NN.N files to live Public/emoji/NN.NN

\* don't copy anything under the "internal" directory.

Check the readme file if it exists, which should say draft.
