(ns forge.world.update
  (:require [anvil.controls :as controls]
            [anvil.fsm :as fsm]
            [anvil.graphics :as g :refer [world-mouse-position]]
            [anvil.stage :as stage]
            [anvil.time :as time]
            [anvil.world :as world :refer [player-eid explored-tile-corners mouseover-entity mouseover-eid active-entities circle->cells point->entities line-of-sight? render-z-order]]
            [clojure.component :as component]
            [clojure.gdx.graphics :refer [delta-time]]
            [clojure.utils :refer [bind-root sort-by-order]]
            [forge.world.potential-fields :refer [update-potential-fields!]]))

; FIXME config/changeable inside the app (dev-menu ?)
(def ^:private ^:dbg-flag pausing? true)

; precaution in case a component gets removed by another component
; the question is do we still want to update nil components ?
; should be contains? check ?
; but then the 'order' is important? in such case dependent components
; should be moved together?
(defn- tick-entity [eid]
  (try
   (doseq [k (keys @eid)]
     (try (when-let [v (k @eid)]
            (component/tick [k v] eid))
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
                     (point->entities
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

(defn- remove-destroyed-entities []
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (world/all-entities))]
    (world/remove-entity eid)
    (doseq [component @eid]
      (component/destroy component eid))))

(defn update-world []
  (component/manual-tick (fsm/state-obj @player-eid))
  (update-mouseover-entity) ; this do always so can get debug info even when game not running
  (bind-root time/paused? (or world/tick-error
                              (and pausing?
                                   (component/pause-game? (fsm/state-obj @player-eid))
                                   (not (controls/unpaused?)))))
  (when-not time/paused?
    (time-update)
    (let [entities (active-entities)]
      (update-potential-fields! entities)
      (try (tick-entities entities)
           (catch Throwable t
             (stage/error-window! t)
             (bind-root world/tick-error t)))))
  (remove-destroyed-entities)) ; do not pause this as for example pickup item, should be destroyed.
