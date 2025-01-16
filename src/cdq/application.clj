(ns cdq.application
  (:require cdq.error
            cdq.time
            cdq.graphics
            clojure.context
            clojure.entity
            clojure.entity.state
            clojure.grid
            clojure.graphics
            clojure.graphics.camera
            clojure.input
            clojure.potential-fields
            clojure.scene2d.actor
            clojure.scene2d.group
            clojure.ui
            clojure.utils
            clojure.world))

(defn draw-stage [{:keys [clojure.context/stage] :as context}]
  (clojure.ui/draw stage (assoc context :clojure.context/unit-scale 1))
  context)

(defn update-stage [context]
  (clojure.ui/act (:clojure.context/stage context) context)
  context)

(defn player-state-input [{:keys [clojure.context/player-eid] :as c}]
  (clojure.entity.state/manual-tick (clojure.entity/state-obj @player-eid) c)
  c)

(defn update-mouseover-entity [{:keys [clojure.context/grid
                                       clojure.context/mouseover-eid
                                       clojure.context/player-eid] :as c}]
  (let [new-eid (if (clojure.context/mouse-on-actor? c)
                  nil
                  (let [player @player-eid
                        hits (remove #(= (:z-order @%) :z-order/effect)
                                     (clojure.grid/point->entities grid (cdq.graphics/world-mouse-position c)))]
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

(defn update-paused [{:keys [clojure.context/player-eid
                             error ; FIXME ! not `::` keys so broken !
                             clojure/input
                             ] :as c}]
  (let [pausing? true]
    (assoc c :clojure.context/paused? (or error
                                          (and pausing?
                                               (clojure.entity.state/pause-game? (clojure.entity/state-obj @player-eid))
                                               (not (or (clojure.input/key-just-pressed? input :p)
                                                        (clojure.input/key-pressed?      input :space))))))))

; TODO how can I write a test for 'space' -> not paused?

(defn when-not-paused [context]
  (if (:clojure.context/paused? context)
    context
    (reduce (fn [context f] (f context))
            context
            [(fn [{:keys [clojure/graphics] :as context}]
               (let [delta-ms (min (clojure.graphics/delta-time graphics)
                                   cdq.time/max-delta)]
                 (-> context
                     (update :clojure.context/elapsed-time + delta-ms)
                     (assoc :clojure.context/delta-time delta-ms))))
             (fn [{:keys [clojure.context/factions-iterations
                          clojure.context/grid
                          world/potential-field-cache
                          cdq.game/active-entities]
                   :as context}]
               (doseq [[faction max-iterations] factions-iterations]
                 (clojure.potential-fields/tick potential-field-cache
                                                grid
                                                faction
                                                active-entities
                                                max-iterations))
               context)
             (fn [{:keys [cdq.game/active-entities] :as context}]
               ; precaution in case a component gets removed by another component
               ; the question is do we still want to update nil components ?
               ; should be contains? check ?
               ; but then the 'order' is important? in such case dependent components
               ; should be moved together?
               (try
                (doseq [eid active-entities]
                  (try
                   (doseq [k (keys @eid)]
                     (try (when-let [v (k @eid)]
                            (clojure.world/tick! [k v] eid context))
                          (catch Throwable t
                            (throw (ex-info "entity-tick" {:k k} t)))))
                   (catch Throwable t
                     (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))
                (catch Throwable t
                  (cdq.error/error-window context t)
                  #_(bind-root ::error t))) ; FIXME ... either reduce or use an atom ...
               context)])))

(defn remove-destroyed-entities [c]
  ; do not pause this as for example pickup item, should be destroyed => make test & remove comment.
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (clojure.world/all-entities c))]
    (clojure.world/remove-entity c eid)
    (doseq [component @eid]
      (clojure.world/destroy! component eid c)))
  c)

(defn camera-controls [{:keys [clojure.graphics/world-viewport
                               clojure/input]
                        :as context}]
  (let [camera (:camera world-viewport)
        zoom-speed 0.025]
    (when (clojure.input/key-pressed? input :minus)  (clojure.graphics.camera/inc-zoom camera    zoom-speed))
    (when (clojure.input/key-pressed? input :equals) (clojure.graphics.camera/inc-zoom camera (- zoom-speed))))
  context)

(defn window-controls [{:keys [clojure/input] :as c}]
  (let [window-hotkeys {:inventory-window   :i
                        :entity-info-window :e}]
    (doseq [window-id [:inventory-window
                       :entity-info-window]
            :when (clojure.input/key-just-pressed? input (get window-hotkeys window-id))]
      (clojure.scene2d.actor/toggle-visible! (get (:windows (:clojure.context/stage c)) window-id))))
  (when (clojure.input/key-just-pressed? input :escape)
    (let [windows (clojure.scene2d.group/children (:windows (:clojure.context/stage c)))]
      (when (some clojure.scene2d.actor/visible? windows)
        (run! #(clojure.scene2d.actor/set-visible % false) windows))))
  c)
