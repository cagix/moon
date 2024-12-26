(ns anvil.world.tick
  (:refer-clojure :exclude [time])
  (:require [anvil.component :as component]
            [anvil.controls :as controls]
            [anvil.entity :as entity]
            [cdq.context :as w]
            [clojure.gdx.graphics :as g]
            [gdl.context :as c]
            [gdl.stage :as stage])
  (:import (com.badlogic.gdx Gdx)))

(defn player-input [{:keys [cdq.context/player-eid] :as c}]
  (component/manual-tick (entity/state-obj @player-eid)
                         c))

(defmethod component/pause-game? :active-skill          [_] false)
(defmethod component/pause-game? :stunned               [_] false)
(defmethod component/pause-game? :player-moving         [_] false)
(defmethod component/pause-game? :player-item-on-cursor [_] true)
(defmethod component/pause-game? :player-idle           [_] true)
(defmethod component/pause-game? :player-dead           [_] true)

(defn- update-paused-state [{:keys [cdq.context/player-eid
                                    cdq.context/error]
                             :as c}
                            pausing?]
  (assoc c :cdq.context/paused? (or error
                                    (and pausing?
                                         (component/pause-game? (entity/state-obj @player-eid))
                                         (not (controls/unpaused?))))))

(defn- calculate-eid [{:keys [cdq.context/player-eid] :as c}]
  (let [player @player-eid
        hits (remove #(= (:z-order @%) :z-order/effect)
                     (w/point->entities c (c/world-mouse-position c)))]
    (->> w/render-z-order
         (sort-by-order hits #(:z-order @%))
         reverse
         (filter #(w/line-of-sight? c player @%))
         first)))

(defn- update-mouseover-entity [{:keys [cdq.context/mouseover-eid] :as c}]
  (let [new-eid (if (stage/mouse-on-actor? c)
                  nil
                  (calculate-eid c))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc c :cdq.context/mouseover-eid new-eid)))

(defn- update-time [c]
  (let [delta-ms (min (g/delta-time Gdx/graphics)
                      w/max-delta-time)]
    (-> c
        (update :cdq.context/elapsed-time + delta-ms)
        (assoc :cdq.context/delta-time delta-ms))))

(defn potential-fields [c])

(defn entities [c])
(defn remove-destroyed-entities [c])
(defn camera-controls [camera])
(defn window-hotkeys  [stage])

(defn-impl w/tick [{:keys [gdl.context/world-viewport] :as c} pausing?]
  (player-input c)
  (let [c (-> c
              update-mouseover-entity
              (update-paused-state pausing?))
        c (if (:cdq.context/paused? c)
            c
            (-> c
                update-time
                potential-fields
                entities))]
    (remove-destroyed-entities c) ; do not pause this as for example pickup item, should be destroyed.
    (camera-controls (:camera world-viewport))
    (window-hotkeys {:controls/close-windows-key controls/close-windows-key
                     :controls/window-hotkeys    controls/window-hotkeys}
                    (stage/get))
    c))
