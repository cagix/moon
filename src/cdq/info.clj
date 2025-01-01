(ns cdq.info
  (:require [anvil.entity :as entity]
            [cdq.context :refer [finished-ratio]]
            [clojure.component :as component]
            [clojure.string :as str]
            [clojure.utils :refer [readable-number k->pretty-name]]
            [gdl.info :as info]))

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
  (str (k->pretty-name k) ": " (entity/stat info/*info-text-entity* k)))

(derive :entity/reaction-time  ::stat)
(derive :entity/movement-speed ::stat)
(derive :entity/strength       ::stat)
(derive :entity/cast-speed     ::stat)
(derive :entity/attack-speed   ::stat)
(derive :entity/armor-save     ::stat)
(derive :entity/armor-pierce   ::stat)

(defmethod component/info :entity/delete-after-duration [counter c]
  (str "Remaining: " (readable-number (finished-ratio c counter)) "/1"))

(defmethod component/info :entity/faction
  [faction _c]
  (str "Faction: " (name faction)))

(defmethod component/info :entity/fsm
  [[_ fsm] _c]
  (str "State: " (name (:state fsm))))

(defmethod component/info :entity/hp
  [_ _c]
  (str "Hitpoints: " (entity/hitpoints info/*info-text-entity*)))

#_(defmethod component/info :entity/skills [skills _c]
  ; => recursive info-text leads to endless text wall
  #_(when (seq skills)
      (str "Skills: " (str/join "," (map name (keys skills))))))

(defmethod component/info :entity/species
  [[_ species] _c]
  (str "Creature - " (str/capitalize (name species))))

(defmethod component/info :entity/temp-modifier
  [[_ {:keys [counter]}] c]
  (str "Spiderweb - remaining: " (readable-number (finished-ratio c counter)) "/1"))
