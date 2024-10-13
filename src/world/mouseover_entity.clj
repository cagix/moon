(ns world.mouseover-entity
  (:require [clojure.gdx.graphics :as g]
            [clojure.gdx.ui.stage-screen :as stage-screen]
            [core.component :refer [defc]]
            [utils.core :refer [sort-by-order]]
            [world.entity :as entity]
            [world.entity.faction :as faction]
            [world.grid :as grid]
            [world.player :refer [world-player]]))

(def ^:private mouseover-entity nil)

(defn mouseover-entity* []
  (when-let [entity mouseover-entity]
    @entity))

(defn- calculate-mouseover-entity []
  (let [player-entity* @world-player
        hits (remove #(= (:z-order %) :z-order/effect) ; or: only items/creatures/projectiles.
                     (map deref
                          (grid/point->entities (g/world-mouse-position))))]
    (->> entity/render-order
         (sort-by-order hits :z-order)
         reverse
         (filter #(entity/line-of-sight? player-entity* %))
         first
         :entity/id)))

(defn update! []
  (let [entity (if (stage-screen/mouse-on-actor?)
                 nil
                 (calculate-mouseover-entity))]
    [(when-let [old-entity mouseover-entity]
       [:e/dissoc old-entity :entity/mouseover?])
     (when entity
       [:e/assoc entity :entity/mouseover? true])
     (fn []
       (.bindRoot #'mouseover-entity entity)
       nil)]))

(defc :entity/clickable
  (entity/render [[_ {:keys [text]}]
           {:keys [entity/mouseover?] :as entity*}]
    (when (and mouseover? text)
      (let [[x y] (:position entity*)]
        (g/draw-text {:text text
                      :x x
                      :y (+ y (:half-height entity*))
                      :up? true})))))

(def ^:private outline-alpha 0.4)
(def ^:private enemy-color    [1 0 0 outline-alpha])
(def ^:private friendly-color [0 1 0 outline-alpha])
(def ^:private neutral-color  [1 1 1 outline-alpha])

(defc :entity/mouseover?
  (entity/render-below [_ {:keys [entity/faction] :as entity*}]
    (let [player-entity* @world-player]
      (g/with-shape-line-width 3
        #(g/draw-ellipse (:position entity*)
                         (:half-width entity*)
                         (:half-height entity*)
                         (cond (= faction (faction/enemy player-entity*))
                               enemy-color
                               (= faction (faction/friend player-entity*))
                               friendly-color
                               :else
                               neutral-color))))))
