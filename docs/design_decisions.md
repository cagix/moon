* Minimize context -> keep the separate steps for initialisation/render/dispose/resize
also separate widgets etc.

    => e.g. spawn-enemies easier to understand w/o context

* Code is easier to understand the less file-context or sourrounding stuff it has

* Evaluate every form separately in which context it belongs (set taskbar icon -> `clojure.java.awt.taskbar`?)

=> but also in the contetx of your app?

=> IT should be trivial to make this game - divide stuff into steps

each non-trivial-form
    (a
        b c ) -> see the dependencies and move there
            -> create namespaces based on all unique dependencies combinations
                -> all the contexts of my app
                    <O.O>

                    !this is the absolute wahnsinn!


### KEEP FILES SMALL AND STEPS SMALL -> DESIGN DECIISION -> SMALLER FILES !!! ## EASIER TO UNDERSTAND
IF NOT LIKE 8000 (but actually it doesnt matter but we need to see all imports etc. )
