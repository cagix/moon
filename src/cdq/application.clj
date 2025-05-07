(ns cdq.application
  (:require [cdq.assets :as assets]
            [cdq.db :as db]
            [cdq.editor :as editor]
            [cdq.entity :as entity]
            [cdq.entity.state :as state]
            [cdq.graphics :as graphics]
            [cdq.graphics.camera :as camera]
            [cdq.grid :as grid]
            [cdq.info :as info]
            [cdq.input :as input]
            [cdq.math.raycaster :as raycaster]
            [cdq.math.shapes :refer [circle->outer-rectangle]]
            [cdq.timer :as timer]
            cdq.potential-fields
            [cdq.ui :as ui :refer [ui-actor]]
            [cdq.ui.action-bar :as action-bar]
            [cdq.ui.stage :as stage]
            [cdq.ui.menu :as ui.menu]
            [cdq.utils :as utils :refer [readable-number pretty-pst sort-by-order]]
            [cdq.val-max :as val-max]
            [cdq.widgets.inventory :as widgets.inventory]
            [cdq.world :as world]
            [clojure.edn :as edn]
            [clojure.gdx.backends.lwjgl :as lwjgl]
            [clojure.java.io :as io]
            [clojure.string :as str])
  (:import (com.badlogic.gdx ApplicationAdapter Gdx)
           (com.badlogic.gdx.graphics Color)
           (com.badlogic.gdx.scenes.scene2d Actor Group)
           (com.badlogic.gdx.utils ScreenUtils)
           (com.badlogic.gdx.utils.viewport Viewport)))

(def ^:private disallowed-keys [:entity/skills
                                #_:entity/fsm
                                :entity/faction
                                :active-skill])

(defn- ->label-text []
  ; items then have 2x pretty-name
  #_(.setText (.getTitleLabel window)
              (if-let [eid world/mouseover-eid]
                (info/text [:property/pretty-name (:property/pretty-name @eid)])
                "Entity Info"))
  (when-let [eid world/mouseover-eid]
    (info/text ; don't use select-keys as it loses Entity record type
               (apply dissoc @eid disallowed-keys))))

(defn- entity-info-window [position]
  (let [label (ui/label "")
        window (ui/window {:title "Info"
                           :id :entity-info-window
                           :visible? false
                           :position position
                           :rows [[{:actor label :expand? true}]]})]
    ; do not change window size ... -> no need to invalidate layout, set the whole stage up again
    ; => fix size somehow.
    (.addActor window (ui-actor {:act (fn []
                                        (.setText label (str (->label-text)))
                                        (.pack window))}))
    window))

(defn- render-infostr-on-bar [infostr x y h]
  (graphics/draw-text {:text infostr
                       :x (+ x 75)
                       :y (+ y 2)
                       :up? true}))

(defn- hp-mana-bar [[x y-mana]]
  (let [rahmen      (graphics/->sprite (assets/get "images/rahmen.png"))
        hpcontent   (graphics/->sprite (assets/get "images/hp.png"))
        manacontent (graphics/->sprite (assets/get "images/mana.png"))
        [rahmenw rahmenh] (:pixel-dimensions rahmen)
        y-hp (+ y-mana rahmenh)
        render-hpmana-bar (fn [x y contentimage minmaxval name]
                            (graphics/draw-image rahmen [x y])
                            (graphics/draw-image (graphics/sub-sprite contentimage [0 0 (* rahmenw (val-max/ratio minmaxval)) rahmenh])
                                                 [x y])
                            (render-infostr-on-bar (str (utils/readable-number (minmaxval 0)) "/" (minmaxval 1) " " name) x y rahmenh))]
    (ui-actor {:draw (fn []
                       (let [player-entity @world/player-eid
                             x (- x (/ rahmenw 2))]
                         (render-hpmana-bar x y-hp   hpcontent   (entity/hitpoints player-entity) "HP")
                         (render-hpmana-bar x y-mana manacontent (entity/mana      player-entity) "MP")))})))

(defn- draw-player-message []
  (when-let [text (:text @stage/player-message)]
    (graphics/draw-text {:x (/ (:width     graphics/ui-viewport) 2)
                         :y (+ (/ (:height graphics/ui-viewport) 2) 200)
                         :text text
                         :scale 2.5
                         :up? true})))

(defn- check-remove-message []
  (when (:text @stage/player-message)
    (swap! stage/player-message update :counter + (.getDeltaTime Gdx/graphics))
    (when (>= (:counter @stage/player-message)
              (:duration-seconds @stage/player-message))
      (swap! stage/player-message dissoc :counter :text))))

(defn- player-message-actor []
  (ui-actor {:draw draw-player-message
             :act  check-remove-message}))

(defn- player-state-actor []
  (ui-actor {:draw #(entity/draw-gui-view (entity/state-obj @world/player-eid))}))

(declare dev-menu-config)

(defn- reset-game! [world-fn]
  (stage/init-state!)
  (stage/clear!)
  (run! stage/add-actor [(ui.menu/create (dev-menu-config))
                         (action-bar/create)
                         (hp-mana-bar [(/ (:width graphics/ui-viewport) 2)
                                       80 ; action-bar-icon-size
                                       ])
                         (ui/group {:id :windows
                                    :actors [(entity-info-window [(:width graphics/ui-viewport) 0])
                                             (widgets.inventory/create [(:width  graphics/ui-viewport)
                                                                        (:height graphics/ui-viewport)])]})
                         (player-state-actor)
                         (player-message-actor)])
  (timer/init!)
  (world/create! ((requiring-resolve world-fn) (db/build-all :properties/creatures))))

(declare paused?)

;"Mouseover-Actor: "
#_(when-let [actor (stage/mouse-on-actor? context)]
    (str "TRUE - name:" (.getName actor)
         "id: " (user-object actor)))

(defn- dev-menu-config []
  {:menus [{:label "World"
            :items (for [world-fn '[cdq.level.vampire/create
                                    cdq.level.uf-caves/create
                                    cdq.level.modules/create]]
                     {:label (str "Start " (namespace world-fn))
                      :on-click (fn [] (reset-game! world-fn))})}
           {:label "Help"
            :items [{:label "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause"}]}
           {:label "Objects"
            :items (for [property-type (sort (filter #(= "properties" (namespace %))
                                                     (keys @#'db/-schemas)))]
                     {:label (str/capitalize (name property-type))
                      :on-click (fn []
                                  (let [window (ui/window {:title "Edit"
                                                           :modal? true
                                                           :close-button? true
                                                           :center? true
                                                           :close-on-escape? true})]
                                    (.add window ^Actor (editor/overview-table property-type editor/edit-property))
                                    (.pack window)
                                    (stage/add-actor window)))})}]
   :update-labels [{:label "Mouseover-entity id"
                    :update-fn (fn []
                                 (when-let [entity (and world/mouseover-eid @world/mouseover-eid)]
                                   (:entity/id entity)))
                    :icon (assets/get "images/mouseover.png")}
                   {:label "elapsed-time"
                    :update-fn (fn [] (str (readable-number timer/elapsed-time) " seconds"))
                    :icon (assets/get "images/clock.png")}
                   {:label "paused?"
                    :update-fn (fn [] paused?)}
                   {:label "GUI"
                    :update-fn (fn [] (graphics/mouse-position))}
                   {:label "World"
                    :update-fn (fn [] (mapv int (graphics/world-mouse-position)))}
                   {:label "Zoom"
                    :update-fn (fn [] (camera/zoom (:camera graphics/world-viewport)))
                    :icon (assets/get "images/zoom.png")}
                   {:label "FPS"
                    :update-fn (fn [] (.getFramesPerSecond Gdx/graphics))
                    :icon (assets/get "images/fps.png")}]})

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

(def ^:private ^:dbg-flag tile-grid? false)
(def ^:private ^:dbg-flag potential-field-colors? false)
(def ^:private ^:dbg-flag cell-entities? false)
(def ^:private ^:dbg-flag cell-occupied? false)

(def ^:private factions-iterations {:good 15 :evil 5})

(defn- draw-before-entities! []
  (let [cam (:camera graphics/world-viewport)
        [left-x right-x bottom-y top-y] (camera/frustum cam)]

    (when tile-grid?
      (graphics/grid (int left-x) (int bottom-y)
                     (inc (int (:width  graphics/world-viewport)))
                     (+ 2 (int (:height graphics/world-viewport)))
                     1 1 [1 1 1 0.8]))

    (doseq [[x y] (camera/visible-tiles cam)
            :let [cell (world/grid [x y])]
            :when cell
            :let [cell* @cell]]

      (when (and cell-entities? (seq (:entities cell*)))
        (graphics/filled-rectangle x y 1 1 [1 0 0 0.6]))

      (when (and cell-occupied? (seq (:occupied cell*)))
        (graphics/filled-rectangle x y 1 1 [0 0 1 0.6]))

      (when potential-field-colors?
        (let [faction :good
              {:keys [distance]} (faction cell*)]
          (when distance
            (let [ratio (/ distance (factions-iterations faction))]
              (graphics/filled-rectangle x y 1 1 [ratio (- 1 ratio) ratio 0.6]))))))))

(defn- geom-test []
  (let [position (graphics/world-mouse-position)
        radius 0.8
        circle {:position position :radius radius}]
    (graphics/circle position radius [1 0 0 0.5])
    (doseq [[x y] (map #(:position @%) (grid/circle->cells world/grid circle))]
      (graphics/rectangle x y 1 1 [1 0 0 0.5]))
    (let [{[x y] :left-bottom :keys [width height]} (circle->outer-rectangle circle)]
      (graphics/rectangle x y width height [0 0 1 1]))))

(def ^:private ^:dbg-flag highlight-blocked-cell? true)

(defn- highlight-mouseover-tile []
  (when highlight-blocked-cell?
    (let [[x y] (mapv int (graphics/world-mouse-position))
          cell (world/grid [x y])]
      (when (and cell (#{:air :none} (:movement @cell)))
        (graphics/rectangle x y 1 1
                                (case (:movement @cell)
                                  :air  [1 1 0 0.5]
                                  :none [1 0 0 0.5]))))))

(defn- draw-after-entities! []
  #_(geom-test)
  (highlight-mouseover-tile))

(def ^:private ^:dbg-flag show-body-bounds false)

(defn- draw-body-rect [entity color]
  (let [[x y] (:left-bottom entity)]
    (graphics/rectangle x y (:width entity) (:height entity) color)))

; I can create this later after loading all the component namespaces
; just go through the systems
; and see which components are signed up for it
; => I get an overview what is rendered how...
#_(def ^:private entity-render-fns
  {:below {:entity/mouseover? draw-faction-ellipse
           :player-item-on-cursor draw-world-item-if-exists
           :stunned draw-stunned-circle}
   :default {:entity/image draw-image-as-of-body
             :entity/clickable draw-text-when-mouseover-and-text
             :entity/line-render draw-line}
   :above {:npc-sleeping draw-zzzz
           :entity/string-effect draw-text
           :entity/temp-modifier draw-filled-circle-grey}
   :info {:entity/hp draw-hpbar-when-mouseover-and-not-full
          :active-skill draw-skill-image-and-active-effect}})

(defn- render-entities! []
  (let [entities (map deref world/active-entities)
        player @world/player-eid]
    (doseq [[z-order entities] (sort-by-order (group-by :z-order entities)
                                              first
                                              world/render-z-order)
            render! [entity/render-below!
                     entity/render-default!
                     entity/render-above!
                     entity/render-info!]
            entity entities
            :when (or (= z-order :z-order/effect)
                      (world/line-of-sight? player entity))]
      (try
       (when show-body-bounds
         (draw-body-rect entity (if (:collides? entity) :white :gray)))
       (doseq [component entity]
         (render! component entity))
       (catch Throwable t
         (draw-body-rect entity :red)
         (pretty-pst t))))))

(defn- update-mouseover-entity! []
  (let [new-eid (if (stage/mouse-on-actor?)
                  nil
                  (let [player @world/player-eid
                        hits (remove #(= (:z-order @%) :z-order/effect)
                                     (grid/point->entities world/grid (graphics/world-mouse-position)))]
                    (->> world/render-z-order
                         (utils/sort-by-order hits #(:z-order @%))
                         reverse
                         (filter #(world/line-of-sight? player @%))
                         first)))]
    (when world/mouseover-eid
      (swap! world/mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (.bindRoot #'world/mouseover-eid new-eid)))

(def pausing? true)

(defn- set-paused-flag! []
  (.bindRoot #'paused? (or #_error
                           (and pausing?
                                (state/pause-game? (entity/state-obj @world/player-eid))
                                (not (or (input/key-just-pressed? :p)
                                         (input/key-pressed?      :space)))))))

(defn- update-time! []
  (let [delta-ms (min (.getDeltaTime Gdx/graphics) world/max-delta)]
    (timer/inc-state! delta-ms)
    (.bindRoot #'world/delta-time delta-ms)))

(defn- update-potential-fields! []
  (doseq [[faction max-iterations] factions-iterations]
    (cdq.potential-fields/tick world/potential-field-cache
                               world/grid
                               faction
                               world/active-entities
                               max-iterations)))

(defn- tick-entities! []
  ; precaution in case a component gets removed by another component
  ; the question is do we still want to update nil components ?
  ; should be contains? check ?
  ; but then the 'order' is important? in such case dependent components
  ; should be moved together?
  (try
   (doseq [eid world/active-entities]
     (try
      (doseq [k (keys @eid)]
        (try (when-let [v (k @eid)]
               (entity/tick! [k v] eid))
             (catch Throwable t
               (throw (ex-info "entity-tick" {:k k} t)))))
      (catch Throwable t
        (throw (ex-info "" (select-keys @eid [:entity/id]) t)))))
   (catch Throwable t
     (stage/error-window! t)
     #_(bind-root ::error t))) ; FIXME ... either reduce or use an atom ...
  )

(defn- camera-controls! []
  (let [camera (:camera graphics/world-viewport)
        zoom-speed 0.025]
    (when (input/key-pressed? :minus)  (camera/inc-zoom camera    zoom-speed))
    (when (input/key-pressed? :equals) (camera/inc-zoom camera (- zoom-speed)))))

(defn- window-controls! []
  (let [window-hotkeys {:inventory-window   :i
                        :entity-info-window :e}]
    (doseq [window-id [:inventory-window
                       :entity-info-window]
            :when (input/key-just-pressed? (get window-hotkeys window-id))]
      (ui/toggle-visible! (get (stage/get-actor :windows) window-id))))
  (when (input/key-just-pressed? :escape)
    (let [windows (Group/.getChildren (stage/get-actor :windows))]
      (when (some Actor/.isVisible windows)
        (run! #(Actor/.setVisible % false) windows)))))

(defn -main []
  (let [config (-> "cdq.application.edn" io/resource slurp edn/read-string)]
    (doseq [ns-sym (:requires config)]
      (require ns-sym))
    (db/create!)
    (lwjgl/application! (:application config)
                        (proxy [ApplicationAdapter] []
                          (create []
                            (assets/create! (:assets config))
                            (graphics/create! (:graphics config))
                            (ui/load! (:vis-ui config)
                                       ; we have to pass batch as we use our draw-image/shapes with our other batch inside stage actors
                                      ; -> tests ?, otherwise could use custom batch also from stage itself and not depend on 'graphics', also pass ui-viewport and dont put in graphics
                                      ) ; TODO we don't do dispose! ....
                            (stage/init!)
                            (reset-game! (:world-fn config)))

                          (dispose []
                            (assets/dispose!)
                            (graphics/dispose!)
                            ; TODO dispose world/tiled-map !! also @ reset-game ?!
                            )

                          (render []
                            (world/cache-active-entities!)
                            (graphics/set-camera-position! (:position @world/player-eid))
                            (ScreenUtils/clear Color/BLACK)
                            (graphics/draw-tiled-map world/tiled-map
                                                     (tile-color-setter world/raycaster
                                                                        world/explored-tile-corners
                                                                        (camera/position (:camera graphics/world-viewport))))
                            (graphics/draw-on-world-view! (fn []
                                                            (draw-before-entities!)
                                                            (render-entities!)
                                                            (draw-after-entities!)))
                            (stage/draw!)
                            (stage/act!)
                            (entity/manual-tick (entity/state-obj @world/player-eid))
                            (update-mouseover-entity!)
                            (set-paused-flag!)
                            (when-not paused?
                              (update-time!)
                              (update-potential-fields!)
                              (tick-entities!))

                            ; do not pause this as for example pickup item, should be destroyed => make test & remove comment.
                            (world/remove-destroyed-entities!)

                            (camera-controls!)
                            (window-controls!))

                          (resize [width height]
                            (Viewport/.update graphics/ui-viewport    width height true)
                            (Viewport/.update graphics/world-viewport width height false))))))
