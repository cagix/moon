(ns moon.entity.skills
  (:require [gdl.utils :refer [readable-number]]
            [moon.component :as component]
            [moon.effect :as effect]
            [moon.entity :as entity]
            [moon.world.time :refer [stopped?]]))

(defc :skill/action-time-modifier-key
  {:schema [:enum :stats/cast-speed :stats/attack-speed]}
  (component/info [[_ v]]
    (str "[VIOLET]" (case v
                      :stats/cast-speed "Spell"
                      :stats/attack-speed "Attack") "[]")))

(defc :skill/action-time {:schema pos?}
  (component/info [[_ v]]
    (str "[GOLD]Action-Time: " (readable-number v) " seconds[]")))

(defc :skill/start-action-sound {:schema :s/sound})

(defc :skill/effects
  {:schema [:s/components-ns :effect]})

(defc :skill/cooldown {:schema nat-int?}
  (component/info [[_ v]]
    (when-not (zero? v)
      (str "[SKY]Cooldown: " (readable-number v) " seconds[]"))))

(defc :skill/cost {:schema nat-int?}
  (component/info [[_ v]]
    (when-not (zero? v)
      (str "[CYAN]Cost: " v " Mana[]"))))

(defc :entity/skills
  {:schema [:s/one-to-many :properties/skills]}
  (entity/create [[k skills] eid]
    (cons [:e/assoc eid k nil]
          (for [skill skills]
            [:tx/add-skill eid skill])))

  (component/info [[_ skills]]
    ; => recursive info-text leads to endless text wall
    #_(when (seq skills)
        (str "[VIOLET]Skills: " (str/join "," (map name (keys skills))) "[]")))

  (entity/tick [[k skills] eid]
    (for [{:keys [skill/cooling-down?] :as skill} (vals skills)
          :when (and cooling-down?
                     (stopped? cooling-down?))]
      [:e/assoc-in eid [k (:property/id skill) :skill/cooling-down?] false])))

(defn has-skill? [{:keys [entity/skills]} {:keys [property/id]}]
  (contains? skills id))

(defc :tx/add-skill
  (component/handle [[_ eid {:keys [property/id] :as skill}]]
    (assert (not (has-skill? @eid skill)))
    [[:e/assoc-in eid [:entity/skills id] skill]
     (when (:entity/player? @eid)
       [:tx.action-bar/add skill])]))

(defc :tx/remove-skill
  (component/handle [[_ eid {:keys [property/id] :as skill}]]
    (assert (has-skill? @eid skill))
    [[:e/dissoc-in eid [:entity/skills id]]
     (when (:entity/player? @eid)
       [:tx.action-bar/remove skill])]))
