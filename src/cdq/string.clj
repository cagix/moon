(ns cdq.string
  (:require [clojure.string :as str]))

(defn pascal->kebab [s]
  (-> s
      (str/replace #"([a-z])([A-Z])" "$1-$2")
      (str/replace #"([A-Z]+)([A-Z][a-z])" "$1-$2")
      (str/lower-case)))
