(ns cdq.render
  (:require cdq.error
            cdq.time
            cdq.stage
            cdq.graphics
            cdq.entity
            cdq.entity.state
            cdq.grid
            clojure.gdx.graphics
            cdq.graphics.camera
            [clojure.gdx.input :as input]
            cdq.potential-fields
            cdq.scene2d.actor
            cdq.scene2d.group
            cdq.ui
            cdq.utils
            cdq.world))

(defn draw-stage [{:keys [cdq.context/stage] :as context}]
  (cdq.ui/draw stage (assoc context :cdq.context/unit-scale 1))
  context)

(defn update-stage [context]
  (cdq.ui/act (:cdq.context/stage context) context)
  context)

(defn player-state-input [{:keys [cdq.context/player-eid] :as c}]
  (cdq.entity.state/manual-tick (cdq.entity/state-obj @player-eid) c)
  c)

(defn update-mouseover-entity [{:keys [cdq.context/grid
                                       cdq.context/mouseover-eid
                                       cdq.context/player-eid] :as c}]
  (let [new-eid (if (cdq.stage/mouse-on-actor? c)
                  nil
                  (let [player @player-eid
                        hits (remove #(= (:z-order @%) :z-order/effect)
                                     (cdq.grid/point->entities grid (cdq.graphics/world-mouse-position c)))]
                    (->> cdq.world/render-z-order
                         (cdq.utils/sort-by-order hits #(:z-order @%))
                         reverse
                         (filter #(cdq.world/line-of-sight? c player @%))
                         first)))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc c :cdq.context/mouseover-eid new-eid)))

(defn update-paused [{:keys [cdq.context/player-eid
                             error ; FIXME ! not `::` keys so broken !
                             ] :as c}]
  (let [pausing? true]
    (assoc c :cdq.context/paused? (or error
                                          (and pausing?
                                               (cdq.entity.state/pause-game? (cdq.entity/state-obj @player-eid))
                                               (not (or (clojure.gdx.input/key-just-pressed? :p)
                                                        (clojure.gdx.input/key-pressed?      :space))))))))

; TODO how can I write a test for 'space' -> not paused?

(defn when-not-paused [context]
  (if (:cdq.context/paused? context)
    context
    (reduce (fn [context f] (f context))
            context
            [(fn [context]
               (let [delta-ms (min (clojure.gdx.graphics/delta-time)
                                   cdq.time/max-delta)]
                 (-> context
                     (update :cdq.context/elapsed-time + delta-ms)
                     (assoc :cdq.context/delta-time delta-ms))))
             (fn [{:keys [cdq.context/factions-iterations
                          cdq.context/grid
                          world/potential-field-cache
                          cdq.game/active-entities]
                   :as context}]
               (doseq [[faction max-iterations] factions-iterations]
                 (cdq.potential-fields/tick potential-field-cache
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
                            (cdq.world/tick! [k v] eid context))
                          (catch Throwable t
                            (throw (ex-info "entity-tick" {:k k} t)))))
                   (catch Throwable t
                     (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))
                (catch Throwable t
                  (cdq.error/error-window context t)
                  #_(bind-root ::error t))) ; FIXME ... either reduce or use an atom ...
               context)])))

(defn remove-destroyed-entities [{:keys [cdq.context/entity-ids] :as c}]
  ; do not pause this as for example pickup item, should be destroyed => make test & remove comment.
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @entity-ids))]
    (cdq.world/remove-entity c eid)
    (doseq [component @eid]
      (cdq.world/destroy! component eid c)))
  c)

(defn camera-controls [{:keys [cdq.graphics/world-viewport]
                        :as context}]
  (let [camera (:camera world-viewport)
        zoom-speed 0.025]
    (when (input/key-pressed? :minus)  (cdq.graphics.camera/inc-zoom camera    zoom-speed))
    (when (input/key-pressed? :equals) (cdq.graphics.camera/inc-zoom camera (- zoom-speed))))
  context)

(defn window-controls [c]
  (let [window-hotkeys {:inventory-window   :i
                        :entity-info-window :e}]
    (doseq [window-id [:inventory-window
                       :entity-info-window]
            :when (input/key-just-pressed? (get window-hotkeys window-id))]
      (cdq.scene2d.actor/toggle-visible! (get (:windows (:cdq.context/stage c)) window-id))))
  (when (input/key-just-pressed? :escape)
    (let [windows (cdq.scene2d.group/children (:windows (:cdq.context/stage c)))]
      (when (some cdq.scene2d.actor/visible? windows)
        (run! #(cdq.scene2d.actor/set-visible % false) windows))))
  c)
