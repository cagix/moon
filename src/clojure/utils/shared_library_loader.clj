(ns clojure.utils.shared-library-loader
  (:import (com.badlogic.gdx.utils SharedLibraryLoader)))

(def mac-osx? SharedLibraryLoader/isMac)
