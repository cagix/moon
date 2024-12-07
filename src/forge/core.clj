(ns forge.core
  (:require [clojure.gdx.graphics :as g :refer [delta-time]]
            [clojure.gdx.scene2d.actor :refer [user-object]]
            [clojure.gdx.scene2d.group :refer [add-actor!
                                               find-actor]]
            [clojure.string :as str]
            [data.grid2d :as g2d]
            [forge.app.asset-manager :refer [play-sound]]
            [forge.app.vis-ui :refer [ui-actor
                                      image-button
                                      text-button
                                      add-tooltip!]
             :as ui]
            [forge.component :refer [info-text]]
            [forge.effect :refer [effects-applicable?]]
            [forge.entity.modifiers :as mods]
            [forge.screens.stage :refer [screen-stage]]
            [forge.val-max :as val-max]
            [forge.world.time :refer [timer reset-timer]]
            [malli.core :as m])
  (:import (com.badlogic.gdx.scenes.scene2d Actor)
           (com.badlogic.gdx.scenes.scene2d.ui Button ButtonGroup)))

(defn- ->pos-int [val-max]
  (mapv #(-> % int (max 0)) val-max))

(defn- apply-max-modifier [val-max entity modifier-k]
  {:pre  [(m/validate val-max/schema val-max)]
   :post [(m/validate val-max/schema val-max)]}
  (let [val-max (update val-max 1 mods/->value entity modifier-k)
        [v mx] (->pos-int val-max)]
    [(min v mx) mx]))

(defn- apply-min-modifier [val-max entity modifier-k]
  {:pre  [(m/validate val-max/schema val-max)]
   :post [(m/validate val-max/schema val-max)]}
  (let [val-max (update val-max 0 mods/->value entity modifier-k)
        [v mx] (->pos-int val-max)]
    [v (max v mx)]))

(defn hitpoints
  "Returns the hitpoints val-max vector `[current-value maximum]` of entity after applying max-hp modifier.
  Current-hp is capped by max-hp."
  [entity]
  (-> entity
      :entity/hp
      (apply-max-modifier entity :modifier/hp-max)))

(defn e-mana
  "Returns the mana val-max vector `[current-value maximum]` of entity after applying max-hp modifier.
  Current-mana is capped by max-mana."
  [entity]
  (-> entity
      :entity/mana
      (apply-max-modifier entity :modifier/mana-max)))

(defn pay-mana-cost [entity cost]
  (let [mana-val ((e-mana entity) 0)]
    (assert (<= cost mana-val))
    (assoc-in entity [:entity/mana 0] (- mana-val cost))))

(defn add-text-effect [entity text]
  (assoc entity
         :entity/string-effect
         (if-let [string-effect (:entity/string-effect entity)]
           (-> string-effect
               (update :text str "\n" text)
               (update :counter reset-timer))
           {:text text
            :counter (timer 0.4)})))

(defn- mana-value [entity]
  (if (:entity/mana entity)
    ((e-mana entity) 0)
    0))

(defn- not-enough-mana? [entity {:keys [skill/cost]}]
  (and cost (> cost (mana-value entity))))

(defn skill-usable-state
  [entity
   {:keys [skill/cooling-down? skill/effects] :as skill}
   effect-ctx]
  (cond
   cooling-down?
   :cooldown

   (not-enough-mana? entity skill)
   :not-enough-mana

   (not (effects-applicable? effect-ctx effects))
   :invalid-params

   :else
   :usable))

(defn has-skill? [{:keys [entity/skills]} {:keys [property/id]}]
  (contains? skills id))

(defn damage-mods
  ([source damage]
   (update damage
           :damage/min-max
           #(-> %
                (apply-min-modifier source :modifier/damage-deal-min)
                (apply-max-modifier source :modifier/damage-deal-max))))

  ([source target damage]
   (update (damage-mods source damage)
           :damage/min-max
           apply-max-modifier
           target
           :modifier/damage-receive-max)))

(defn- action-bar-button-group []
  (let [actor (ui-actor {})]
    (.setName actor "action-bar/button-group")
    (Actor/.setUserObject actor (ui/button-group {:max-check-count 1
                                                  :min-check-count 0}))
    actor))

(defn- group->button-group [group]
  (user-object (find-actor group "action-bar/button-group")))

(defn- get-action-bar []
  (let [group (::action-bar (:action-bar-table (screen-stage)))]
    {:horizontal-group group
     :button-group (group->button-group group)}))

(defn actionbar-add-skill [{:keys [property/id entity/image] :as skill}]
  (let [{:keys [horizontal-group button-group]} (get-action-bar)
        button (image-button image (fn []) {:scale 2})]
    (Actor/.setUserObject button id)
    (add-tooltip! button #(info-text skill)) ; (assoc ctx :effect/source (world/player)) FIXME
    (add-actor! horizontal-group button)
    (ButtonGroup/.add button-group button)
    nil))

(defn actionbar-remove-skill [{:keys [property/id]}]
  (let [{:keys [horizontal-group button-group]} (get-action-bar)
        ^Button button (get horizontal-group id)]
    (.remove button)
    (ButtonGroup/.remove button-group button)
    nil))

(defn actionbar-create []
  (let [group (ui/horizontal-group {:pad 2 :space 2})]
    (.setUserObject group ::action-bar)
    (add-actor! group (action-bar-button-group))
    group))

(defn actionbar-selected-skill []
  (when-let [skill-button (ButtonGroup/.getChecked (:button-group (get-action-bar)))]
    (user-object skill-button)))

(defn add-skill [entity {:keys [property/id] :as skill}]
  {:pre [(not (has-skill? entity skill))]}
  (when (:entity/player? entity)
    (actionbar-add-skill skill))
  (assoc-in entity [:entity/skills id] skill))

(defn remove-skill [entity {:keys [property/id] :as skill}]
  {:pre [(has-skill? entity skill)]}
  (when (:entity/player? entity)
    (actionbar-remove-skill skill))
  (update entity :entity/skills dissoc id))
