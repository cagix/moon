(ns forge.world.update
  (:require [anvil.controls :as controls]
            [anvil.entity :as entity :refer [player-eid mouseover-entity mouseover-eid line-of-sight? render-z-order]]
            [anvil.error :as error]
            [anvil.fsm :as fsm]
            [anvil.graphics :as g :refer [world-mouse-position]]
            [anvil.grid :as grid]
            [anvil.stage :as stage]
            [anvil.time :as time]
            [anvil.level :as level :refer [explored-tile-corners]]
            [clojure.component :refer [defsystem]]
            [clojure.gdx.graphics :refer [delta-time]]
            [clojure.utils :refer [bind-root sort-by-order]]
            [forge.world.potential-fields :refer [update-potential-fields!]]))

; FIXME config/changeable inside the app (dev-menu ?)
(def ^:private ^:dbg-flag pausing? true)

(defsystem tick)
(defmethod tick :default [_ eid])

; precaution in case a component gets removed by another component
; the question is do we still want to update nil components ?
; should be contains? check ?
; but then the 'order' is important? in such case dependent components
; should be moved together?
(defn- tick-entity [eid]
  (try
   (doseq [k (keys @eid)]
     (try (when-let [v (k @eid)]
            (tick [k v] eid))
          (catch Throwable t
            (throw (ex-info "entity-tick" {:k k} t)))))
   (catch Throwable t
     (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))

(defn- tick-entities [entities]
  (run! tick-entity entities))

(defn- time-update []
  (let [delta-ms (min (delta-time) time/max-delta)]
    (alter-var-root #'time/elapsed + delta-ms)
    (bind-root time/delta delta-ms)))

(defn- calculate-eid []
  (let [player @player-eid
        hits (remove #(= (:z-order @%) :z-order/effect)
                     (grid/point->entities
                      (world-mouse-position)))]
    (->> render-z-order
         (sort-by-order hits #(:z-order @%))
         reverse
         (filter #(line-of-sight? player @%))
         first)))

(defn- update-mouseover-entity []
  (let [new-eid (if (stage/mouse-on-actor?)
                  nil
                  (calculate-eid))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (bind-root mouseover-eid new-eid)))

(defsystem destroy)
(defmethod destroy :default [_ eid])

(defn- remove-destroyed-entities []
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (entity/all-entities))]
    (entity/remove-entity eid)
    (doseq [component @eid]
      (destroy component eid))))

(defsystem manual-tick)
(defmethod manual-tick :default [_])

(defsystem pause-game?)
(defmethod pause-game? :default [_])

(defn update-world []
  (manual-tick (fsm/state-obj @player-eid))
  (update-mouseover-entity) ; this do always so can get debug info even when game not running
  (bind-root time/paused? (or error/throwable
                              (and pausing?
                                   (pause-game? (fsm/state-obj @player-eid))
                                   (not (controls/unpaused?)))))
  (when-not time/paused?
    (time-update)
    (let [entities (entity/active-entities)]
      (update-potential-fields! entities)
      (try (tick-entities entities)
           (catch Throwable t
             (stage/error-window! t)
             (bind-root error/throwable t)))))
  (remove-destroyed-entities)) ; do not pause this as for example pickup item, should be destroyed.
