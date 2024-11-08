(ns moon.effect.entity.convert
  (:require [moon.effect :refer [source target]]
            [moon.entity.faction :as faction]))

(defn info [_]
  "Converts target to your side.")

(defn applicable? [_]
  (and target
       (= (:entity/faction @target)
          (faction/enemy @source))))

(defn handle [_]
  [[:tx/audiovisual (:position @target) :audiovisuals/convert]
   [:e/assoc target :entity/faction (:entity/faction @source)]])
