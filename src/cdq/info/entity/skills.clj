(ns cdq.info.entity.skills
  (:require [clojure.string :as str]))

(defn info-segment [[_ skills] _ctx]
  ; => recursive info-text leads to endless text wall
  (when (seq skills)
    (str "Skills: " (str/join "," (map name (keys skills))))))
