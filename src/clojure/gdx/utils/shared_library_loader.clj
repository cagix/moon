(ns clojure.gdx.utils.shared-library-loader
  (:import (com.badlogic.gdx.utils SharedLibraryLoader)))

(defn bitness []
  SharedLibraryLoader/bitness)

(defn architecture []
  SharedLibraryLoader/architecture)

(defn os []
  SharedLibraryLoader/os)
