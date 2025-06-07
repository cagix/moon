(ns gdl.start
  (:require [clojure.gdx.app-listener :as app-listener]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.gdx.utils.shared-library-loader :as shared-library-loader]
            [clojure.gdx.utils.os :as os]))

; TODO GAVE WRONG OPTION AND DIDNT COMPLAIN
; GET STATIC FIELD CLOJURE.JAVA.INTEROP
(defn operating-system []
  (get os/mapping (shared-library-loader/os)))

(defn start! [{:keys [config listener]}]
  (lwjgl/application! config
                      (app-listener/create-adapter listener)))
