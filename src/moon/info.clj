(ns moon.info
  (:require [clojure.string :as str]
            [gdl.graphics.color :as color]
            [gdl.info :as info :refer [info]]
            [gdl.utils :refer [k->pretty-name readable-number]]
            [moon.entity.stat :as stat]))

(color/put "MODIFIERS" :cyan)
(color/put "PRETTY_NAME" [0.84 0.8 0.52])

(defmethod info :property/pretty-name [[_ value]]
  (str "[PRETTY_NAME]"value"[]"))

(defmethod info :maxrange [[_ maxrange]]
  (str "[LIGHT_GRAY]Range " maxrange " meters[]"))

(defmethod info :creature/species [[_ species]]
  (str "[LIGHT_GRAY]Creature - " (str/capitalize (name species)) "[]"))

(defmethod info :creature/level [[_ lvl]]
  (str "[GRAY]Level " lvl "[]"))

(defmethod info :projectile/piercing? [_] ; TODO also when false ?!
  "[LIME]Piercing[]")

(defmethod info :skill/action-time-modifier-key [[_ v]]
  (str "[VIOLET]" (case v
                    :entity/cast-speed "Spell"
                    :entity/attack-speed "Attack") "[]"))

(defmethod info :skill/action-time [[_ v]]
  (str "[GOLD]Action-Time: " (readable-number v) " seconds[]"))

(defmethod info :skill/cooldown [[_ v]]
  (when-not (zero? v)
    (str "[SKY]Cooldown: " (readable-number v) " seconds[]")))

(defmethod info :skill/cost [[_ v]]
  (when-not (zero? v)
    (str "[CYAN]Cost: " v " Mana[]")))

(defmethod info ::stat [[k _]]
  (str (k->pretty-name k) ": " (stat/value info/*entity* k)))

(derive :entity/reaction-time  ::stat)
(derive :entity/movement-speed ::stat)
(derive :entity/strength       ::stat)
(derive :entity/cast-speed     ::stat)
(derive :entity/attack-speed   ::stat)
(derive :entity/armor-save     ::stat)
(derive :entity/armor-pierce   ::stat)
