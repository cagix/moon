(ns clojure.gdx.utils.shared-library-loader
  (:import (com.badlogic.gdx.utils SharedLibraryLoader)))

(defn os []
  (cond
   (= SharedLibraryLoader/os com.badlogic.gdx.utils.Os/MacOsX)
   :mac-osx))

