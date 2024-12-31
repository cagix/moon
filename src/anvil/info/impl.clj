(ns anvil.info.impl
  (:require [anvil.component :as component]
            [anvil.entity :as entity]
            [anvil.info :as info]
            [clojure.utils :refer [readable-number]]))

(defmethod component/info :property/pretty-name [[_ v] _c] v)
(defmethod component/info :maxrange             [[_ v] _c] v)

(defmethod component/info :creature/level [[_ v] _c]
  (str "Level: " v))

(defmethod component/info :projectile/piercing? [_ _c] ; TODO also when false ?!
  "Piercing")

(defmethod component/info :skill/action-time-modifier-key [[_ v] _c]
  (case v
    :entity/cast-speed "Spell"
    :entity/attack-speed "Attack"))

(defmethod component/info :skill/action-time [[_ v] _c]
  (str "Action-Time: " (readable-number v) " seconds"))

(defmethod component/info :skill/cooldown [[_ v] _c]
  (when-not (zero? v)
    (str "Cooldown: " (readable-number v) " seconds")))

(defmethod component/info :skill/cost [[_ v] _c]
  (when-not (zero? v)
    (str "Cost: " v " Mana")))

(defmethod component/info ::stat [[k _] _c]
  (str (info/k->pretty-name k) ": " (entity/stat info/*info-text-entity* k)))

(derive :entity/reaction-time  ::stat)
(derive :entity/movement-speed ::stat)
(derive :entity/strength       ::stat)
(derive :entity/cast-speed     ::stat)
(derive :entity/attack-speed   ::stat)
(derive :entity/armor-save     ::stat)
(derive :entity/armor-pierce   ::stat)
