(ns clojure.gdx.utils.shared-library-loader
  (:require [clojure.gdx.utils.os :as os])
  (:import (com.badlogic.gdx.utils SharedLibraryLoader)))

(defn bitness []
  SharedLibraryLoader/bitness)

(defn architecture []
  SharedLibraryLoader/architecture)

; TODO GAVE WRONG OPTION AND DIDNT COMPLAIN
; GET STATIC FIELD CLOJURE.JAVA.INTEROP
(defn os []
  (get os/mapping SharedLibraryLoader/os))
