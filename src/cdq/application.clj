(ns cdq.application
  (:require clojure.context
            clojure.entity
            clojure.entity.state
            clojure.graphics
            clojure.graphics.camera
            clojure.graphics.shape-drawer
            clojure.graphics.tiled-map
            clojure.input
            clojure.potential-fields
            clojure.scene2d.actor
            clojure.scene2d.group
            clojure.ui
            clojure.utils
            clojure.world
            clojure.world.graphics)
  (:import (com.badlogic.gdx.utils ScreenUtils)))

(def render-fns
  [(fn [{:keys [clojure.graphics/world-viewport
                clojure.context/player-eid]
         :as context}]
     {:pre [world-viewport
            player-eid]}
     (clojure.graphics.camera/set-position (:camera world-viewport)
                                           (:position @player-eid))
     context)
   (fn [context]
     (ScreenUtils/clear com.badlogic.gdx.graphics.Color/BLACK)
     context)
   (fn [{:keys [clojure.graphics/world-viewport
                clojure.context/tiled-map
                clojure.context/raycaster
                clojure.context/explored-tile-corners]
         :as context}]
     (clojure.context/draw-tiled-map context
                                     tiled-map
                                     (clojure.graphics.tiled-map/tile-color-setter raycaster
                                                                                   explored-tile-corners
                                                                                   (clojure.graphics.camera/position (:camera world-viewport))))
     context)
   (fn [context]
     (let [render-fns [clojure.world.graphics/render-before-entities
                       clojure.world.graphics/render-entities
                       clojure.world.graphics/render-after-entities]]
       (clojure.context/draw-on-world-view context
                                           (fn [context]
                                             (doseq [f render-fns]
                                               (f context)))))
     context)
   (fn [{:keys [clojure.context/stage] :as context}]
     (clojure.ui/draw stage (assoc context :clojure.context/unit-scale 1))
     context)
   (fn [context]
     (clojure.ui/act (:clojure.context/stage context) context)
     context)
   (fn [{:keys [clojure.context/player-eid] :as c}]
     (clojure.entity.state/manual-tick (clojure.entity/state-obj @player-eid) c)
     c)
   (fn [{:keys [clojure.context/mouseover-eid
                clojure.context/player-eid] :as c}]
     (let [new-eid (if (clojure.context/mouse-on-actor? c)
                     nil
                     (let [player @player-eid
                           hits (remove #(= (:z-order @%) :z-order/effect)
                                        (clojure.world/point->entities c (clojure.context/world-mouse-position c)))]
                       (->> clojure.world/render-z-order
                            (clojure.utils/sort-by-order hits #(:z-order @%))
                            reverse
                            (filter #(clojure.world/line-of-sight? c player @%))
                            first)))]
       (when mouseover-eid
         (swap! mouseover-eid dissoc :entity/mouseover?))
       (when new-eid
         (swap! new-eid assoc :entity/mouseover? true))
       (assoc c :clojure.context/mouseover-eid new-eid)))
   (fn [{:keys [clojure.context/player-eid
                error ; FIXME ! not `::` keys so broken !
                ] :as c}]
     (let [pausing? true]
       (assoc c :clojure.context/paused? (or error
                                             (and pausing?
                                                  (clojure.entity.state/pause-game? (clojure.entity/state-obj @player-eid))
                                                  (not (or (clojure.input/key-just-pressed? :p)
                                                           (clojure.input/key-pressed? :space))))))))
   (fn [context]
     (if (:clojure.context/paused? context)
       context
       (reduce (fn [context f] (f context))
               context
               [(fn [context]
                  (let [delta-ms (min (clojure.graphics/delta-time) clojure.world/max-delta-time)]
                    (-> context
                        (update :clojure.context/elapsed-time + delta-ms)
                        (assoc :clojure.context/delta-time delta-ms))))
                (fn [{:keys [clojure.context/factions-iterations
                             clojure.context/grid
                             world/potential-field-cache]
                      :as c}]
                  (let [entities (clojure.world/active-entities c)]
                    (doseq [[faction max-iterations] factions-iterations]
                      (clojure.potential-fields/tick potential-field-cache
                                                     grid
                                                     faction
                                                     entities
                                                     max-iterations)))
                  c)
                (fn [c]
                  ; precaution in case a component gets removed by another component
                  ; the question is do we still want to update nil components ?
                  ; should be contains? check ?
                  ; but then the 'order' is important? in such case dependent components
                  ; should be moved together?
                  (try
                   (doseq [eid (clojure.world/active-entities c)]
                     (try
                      (doseq [k (keys @eid)]
                        (try (when-let [v (k @eid)]
                               (clojure.world/tick! [k v] eid c))
                             (catch Throwable t
                               (throw (ex-info "entity-tick" {:k k} t)))))
                      (catch Throwable t
                        (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))
                   (catch Throwable t
                     (clojure.context/error-window c t)
                     #_(bind-root ::error t))) ; FIXME ... either reduce or use an atom ...
                  c)])))
   (fn [c]
     ; do not pause this as for example pickup item, should be destroyed => make test & remove comment.
     (doseq [eid (filter (comp :entity/destroyed? deref)
                         (clojure.world/all-entities c))]
       (clojure.world/remove-entity c eid)
       (doseq [component @eid]
         (clojure.world/destroy! component eid c)))
     c)
   (fn [{:keys [clojure.graphics/world-viewport]
         :as context}]
     (let [camera (:camera world-viewport)
           zoom-speed 0.025]
       (when (clojure.input/key-pressed? :minus)  (clojure.graphics.camera/inc-zoom camera    zoom-speed))
       (when (clojure.input/key-pressed? :equals) (clojure.graphics.camera/inc-zoom camera (- zoom-speed))))
     context)
   (fn [c]
     (let [window-hotkeys {:inventory-window   :i
                           :entity-info-window :e}]
       (doseq [window-id [:inventory-window
                          :entity-info-window]
               :when (clojure.input/key-just-pressed? (get window-hotkeys window-id))]
         (clojure.scene2d.actor/toggle-visible! (get (:windows (:clojure.context/stage c)) window-id))))
     (when (clojure.input/key-just-pressed? :escape)
       (let [windows (clojure.scene2d.group/children (:windows (:clojure.context/stage c)))]
         (when (some clojure.scene2d.actor/visible? windows)
           (run! #(clojure.scene2d.actor/set-visible % false) windows))))
     c)])
