(ns clojure.gdx.interop
  (:require [clojure.string :as str]))

(defn static-field [klass-str k]
  (eval (symbol (str "com.badlogic.gdx." klass-str "/" (str/replace (str/upper-case (name k)) "-" "_")))))
