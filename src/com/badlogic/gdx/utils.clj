(ns com.badlogic.gdx.utils
  (:require [com.badlogic.gdx.utils.os :as os]
            [com.badlogic.gdx.utils.shared-library-loader :as shared-library-loader]))

(defn operating-system []
  (os/value->keyword shared-library-loader/os))
