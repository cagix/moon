(ns cdq.info
  (:require [cdq.entity :as entity]
            [clojure.string :as str]
            [gdl.utils :refer [readable-number]]
            [gdl.info :as info]))

(defmethod info/text :property/pretty-name [[_ v] _entity _c] v)
(defmethod info/text :maxrange             [[_ v] _entity _c] v)

(defmethod info/text :creature/level [[_ v] _entity _c]
  (str "Level: " v))

(defmethod info/text :projectile/piercing? [_ _entity _c] ; TODO also when false ?!
  "Piercing")

(defmethod info/text :skill/action-time-modifier-key [[_ v] _entity _c]
  (case v
    :entity/cast-speed "Spell"
    :entity/attack-speed "Attack"))

(defmethod info/text :skill/action-time [[_ v] _entity _c]
  (str "Action-Time: " (readable-number v) " seconds"))

(defmethod info/text :skill/cooldown [[_ v] _entity _c]
  (when-not (zero? v)
    (str "Cooldown: " (readable-number v) " seconds")))

(defmethod info/text :skill/cost [[_ v] _entity _c]
  (when-not (zero? v)
    (str "Cost: " v " Mana")))

(defmethod info/text ::stat [[k _] entity _c]
  (str (str/capitalize (name k)) ": " (entity/stat entity k)))

(derive :entity/reaction-time  ::stat)
(derive :entity/movement-speed ::stat)
(derive :entity/strength       ::stat)
(derive :entity/cast-speed     ::stat)
(derive :entity/attack-speed   ::stat)
(derive :entity/armor-save     ::stat)
(derive :entity/armor-pierce   ::stat)
