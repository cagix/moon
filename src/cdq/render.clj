(ns cdq.render
  (:require [cdq.context :as context]
            [gdl.graphics.camera :as camera]
            [gdl.graphics.shape-drawer :as sd]
            [gdl.graphics.tiled-map-renderer :as tiled-map-renderer]
            cdq.grid
            [cdq.line-of-sight :as los]
            cdq.render.player-state-input
            [cdq.stage :as stage]
            [gdl.math.raycaster :as raycaster]
            cdq.world
            [gdl.data.grid2d :as g2d]
            [gdl.input :as input]
            [gdl.ui.actor :as actor]
            [gdl.ui.group :as group]
            [gdl.utils :as utils])
  (:import (com.badlogic.gdx.graphics Color)
           (com.badlogic.gdx.graphics.g2d Batch)
           (gdl StageWithState)))

(defn- active-entities [{:keys [grid]} center-entity]
  (->> (let [idx (-> center-entity
                     :cdq.content-grid/content-cell
                     deref
                     :idx)]
         (cons idx (g2d/get-8-neighbour-positions idx)))
       (keep grid)
       (mapcat (comp :entities deref))))

(defn- assoc-active-entities [{:keys [cdq.context/content-grid
                                      cdq.context/player-eid]
                               :as context}]
  (assoc context :cdq.game/active-entities (active-entities content-grid @player-eid)))

(defn- set-camera-on-player
  [{:keys [gdl.graphics/world-viewport
           cdq.context/player-eid]
    :as context}]
  {:pre [world-viewport
         player-eid]}
  (camera/set-position (:camera world-viewport)
                       (:position @player-eid))
  context)

(defn- clear-screen! [context]
  (com.badlogic.gdx.utils.ScreenUtils/clear com.badlogic.gdx.graphics.Color/BLACK)
  context)

(def ^:private explored-tile-color (Color. (float 0.5) (float 0.5) (float 0.5) (float 1)))

(def ^:private ^:dbg-flag see-all-tiles? false)

(comment
 (def ^:private count-rays? false)

 (def ray-positions (atom []))
 (def do-once (atom true))

 (count @ray-positions)
 2256
 (count (distinct @ray-positions))
 608
 (* 608 4)
 2432
 )

(defn- tile-color-setter [raycaster explored-tile-corners light-position]
  #_(reset! do-once false)
  (let [light-cache (atom {})]
    (fn tile-color-setter [_color x y]
      (let [position [(int x) (int y)]
            explored? (get @explored-tile-corners position) ; TODO needs int call ?
            base-color (if explored? explored-tile-color Color/BLACK)
            cache-entry (get @light-cache position :not-found)
            blocked? (if (= cache-entry :not-found)
                       (let [blocked? (raycaster/blocked? raycaster light-position position)]
                         (swap! light-cache assoc position blocked?)
                         blocked?)
                       cache-entry)]
        #_(when @do-once
            (swap! ray-positions conj position))
        (if blocked?
          (if see-all-tiles? Color/WHITE base-color)
          (do (when-not explored?
                (swap! explored-tile-corners assoc (mapv int position) true))
              Color/WHITE))))))

(defn- render-tiled-map! [{:keys [gdl.graphics/world-viewport
                                  cdq.context/tiled-map
                                  cdq.context/raycaster
                                  cdq.context/explored-tile-corners]
                           :as context}]
  (tiled-map-renderer/draw context
                           tiled-map
                           (tile-color-setter raycaster
                                              explored-tile-corners
                                              (camera/position (:camera world-viewport))))
  context)

(def ^:private render-fns
  '[(cdq.render.draw-on-world-view.before-entities/render)
    (cdq.render.draw-on-world-view.entities/render-entities
     {:below {:entity/mouseover? cdq.render.draw-on-world-view.entities/draw-faction-ellipse
              :player-item-on-cursor cdq.render.draw-on-world-view.entities/draw-world-item-if-exists
              :stunned cdq.render.draw-on-world-view.entities/draw-stunned-circle}
      :default {:entity/image cdq.render.draw-on-world-view.entities/draw-image-as-of-body
                :entity/clickable cdq.render.draw-on-world-view.entities/draw-text-when-mouseover-and-text
                :entity/line-render cdq.render.draw-on-world-view.entities/draw-line}
      :above {:npc-sleeping cdq.render.draw-on-world-view.entities/draw-zzzz
              :entity/string-effect cdq.render.draw-on-world-view.entities/draw-text
              :entity/temp-modifier cdq.render.draw-on-world-view.entities/draw-filled-circle-grey}
      :info {:entity/hp cdq.render.draw-on-world-view.entities/draw-hpbar-when-mouseover-and-not-full
             :active-skill cdq.render.draw-on-world-view.entities/draw-skill-image-and-active-effect}})
    (cdq.render.draw-on-world-view.after-entities/render)])

(defn- draw-with [{:keys [^Batch gdl.graphics/batch
                          gdl.graphics/shape-drawer] :as c}
                 viewport
                 unit-scale
                 draw-fn]
  (.setColor batch Color/WHITE) ; fix scene2d.ui.tooltip flickering
  (.setProjectionMatrix batch (camera/combined (:camera viewport)))
  (.begin batch)
  (sd/with-line-width shape-drawer unit-scale
    (fn []
      (draw-fn (assoc c :cdq.context/unit-scale unit-scale))))
  (.end batch))

(defn- draw-on-world-view* [{:keys [gdl.graphics/world-unit-scale
                                    gdl.graphics/world-viewport] :as c} render-fn]
  (draw-with c
             world-viewport
             world-unit-scale
             render-fn))

(defn- draw-on-world-view! [context]
  (draw-on-world-view* context
                       (fn [context]
                         (doseq [f render-fns]
                           (utils/req-resolve-call f context))))
  context)

(defn- render-stage! [{:keys [^StageWithState cdq.context/stage] :as context}]
  (set! (.applicationState stage) (assoc context :cdq.context/unit-scale 1))
  (com.badlogic.gdx.scenes.scene2d.Stage/.draw stage)
  (set! (.applicationState stage) context)
  (com.badlogic.gdx.scenes.scene2d.Stage/.act stage)
  context)

(defn- update-mouseover-entity! [{:keys [cdq.context/grid
                                         cdq.context/mouseover-eid
                                         cdq.context/player-eid
                                         gdl.graphics/world-viewport
                                         cdq.context/stage] :as c}]
  (let [new-eid (if (stage/mouse-on-actor? stage)
                  nil
                  (let [player @player-eid
                        hits (remove #(= (:z-order @%) :z-order/effect)
                                     (cdq.grid/point->entities grid (gdl.graphics/world-mouse-position world-viewport)))]
                    (->> cdq.world/render-z-order
                         (gdl.utils/sort-by-order hits #(:z-order @%))
                         reverse
                         (filter #(los/exists? c player @%))
                         first)))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc c :cdq.context/mouseover-eid new-eid)))

(defn- update-paused! [{:keys [cdq.context/player-eid
                               context/entity-components
                               error ; FIXME ! not `::` keys so broken !
                               ] :as c}]
  (let [pausing? true]
    (assoc c :cdq.context/paused? (or error
                                      (and pausing?
                                           (get-in entity-components [(cdq.entity/state-k @player-eid) :pause-game?])
                                           (not (or (input/key-just-pressed? :p)
                                                    (input/key-pressed?      :space))))))))

(defn- when-not-paused! [context]
  (if (:cdq.context/paused? context)
    context
    (reduce (fn [context f]
              (gdl.utils/req-resolve-call f context))
            context
            '[(cdq.render.when-not-paused.update-time/render)
              (cdq.render.when-not-paused.update-potential-fields/render)
              (cdq.render.when-not-paused.tick-entities/render)])))

(defn- remove-destroyed-entities! [{:keys [cdq.context/entity-ids
                                           context/entity-components]
                                    :as context}]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @entity-ids))]
    (doseq [component context]
      (context/remove-entity component eid))
    (doseq [[k v] @eid
            :let [destroy! (get-in entity-components [k :destroy!])]
            :when destroy!]
      (destroy! v eid context)))
  context)

(defn- camera-controls! [{:keys [gdl.graphics/world-viewport]
                          :as context}]
  (let [camera (:camera world-viewport)
        zoom-speed 0.025]
    (when (input/key-pressed? :minus)  (camera/inc-zoom camera    zoom-speed))
    (when (input/key-pressed? :equals) (camera/inc-zoom camera (- zoom-speed))))
  context)

(defn- window-controls! [c]
  (let [window-hotkeys {:inventory-window   :i
                        :entity-info-window :e}]
    (doseq [window-id [:inventory-window
                       :entity-info-window]
            :when (input/key-just-pressed? (get window-hotkeys window-id))]
      (actor/toggle-visible! (get (:windows (:cdq.context/stage c)) window-id))))
  (when (input/key-just-pressed? :escape)
    (let [windows (group/children (:windows (:cdq.context/stage c)))]
      (when (some actor/visible? windows)
        (run! #(actor/set-visible % false) windows))))
  c)

(defn game-loop! [context]
  (reduce (fn [context f]
            (f context))
          context
          [assoc-active-entities
           set-camera-on-player
           clear-screen!
           render-tiled-map!
           draw-on-world-view!
           render-stage!
           cdq.render.player-state-input/render
           update-mouseover-entity!
           update-paused!
           when-not-paused!

           ; do not pause this as for example pickup item, should be destroyed => make test & remove comment.
           remove-destroyed-entities!

           camera-controls!
           window-controls!]))
