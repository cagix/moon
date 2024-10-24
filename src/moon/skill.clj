(ns moon.skill
  (:require [gdl.utils :refer [readable-number]]
            [moon.component :refer [defc] :as component]
            [moon.effect :as effect]
            [moon.entity :as entity]
            [moon.entity.state :as state]
            [moon.property :as property]
            [moon.world :as world :refer [stopped?]]))

(property/def :properties/skills
  {:schema [:entity/image
            :property/pretty-name
            :skill/action-time-modifier-key
            :skill/action-time
            :skill/start-action-sound
            :skill/effects
            [:skill/cooldown {:optional true}]
            [:skill/cost {:optional true}]]
   :overview {:title "Skills"
              :columns 16
              :image/scale 2}})

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

(defn- has-skill? [{:keys [entity/skills]} {:keys [property/id]}]
  (contains? skills id))

(.bindRoot #'entity/has-skill? has-skill?)

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

(defn- player-clicked-skillmenu [skill]
  (state/clicked-skillmenu-skill (state/state-obj @world/player) skill))

; TODO render text label free-skill-points
; (str "Free points: " (:entity/free-skill-points @world/player))
#_(defn ->skill-window []
    (ui/window {:title "Skills"
                :id :skill-window
                :visible? false
                :cell-defaults {:pad 10}
                :rows [(for [id [:skills/projectile
                                 :skills/meditation
                                 :skills/spawn
                                 :skills/melee-attack]
                             :let [; get-property in callbacks if they get changed, this is part of context permanently
                                   button (ui/image-button ; TODO reuse actionbar button scale?
                                                           (:entity/image (db/get id)) ; TODO here anyway taken
                                                           ; => should probably build this window @ game start
                                                           (fn []
                                                             (tx/do! (player-clicked-skillmenu (db/get id)))))]]
                         (do
                          (ui/add-tooltip! button #(info/->text (db/get id))) ; TODO no player modifiers applied (see actionbar)
                          button))]
                :pack? true}))

(defn- mana-value [entity]
  (if-let [mana (entity/stat entity :stats/mana)]
    (mana 0)
    0))

(defn- not-enough-mana? [entity {:keys [skill/cost]}]
  (> cost (mana-value entity)))

(defn- usable-state
  [entity {:keys [skill/cooling-down? skill/effects] :as skill}]
  (cond
   cooling-down?
   :cooldown

   (not-enough-mana? entity skill)
   :not-enough-mana

   (not (effect/effect-applicable? effects))
   :invalid-params

   :else
   :usable))

(.bindRoot #'entity/skill-usable-state usable-state)
