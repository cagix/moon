(ns cdq.game
  (:require cdq.entity-api
            [cdq.ctx.audio :as audio]
            cdq.interaction-state
            [cdq.ctx.db :as db]
            [cdq.dev.data-view :as data-view]
            [cdq.world.effect :as effect]
            [cdq.world.entity :as entity]
            [cdq.entity.fsm :as fsm]
            [cdq.entity.timers :as timers]
            cdq.entity.state.player-idle
            cdq.entity.state.player-item-on-cursor
            cdq.entity.state.player-moving
            [cdq.ctx.graphics :as graphics]
            [cdq.gdx.graphics.camera :as camera]
            [cdq.world.grid :as grid]
            [cdq.ctx.input :as input]
            [cdq.inventory :as inventory]
            [cdq.malli :as m]
            [cdq.gdx.math.geom :as geom]
            [cdq.world.entity.stats :as modifiers]
            [cdq.raycaster :as raycaster]
            [cdq.stacktrace :as stacktrace]
            [cdq.tile-color-setter :as tile-color-setter]
            [cdq.timer :as timer]
            [cdq.op :as op]
            [cdq.ctx.stage :as stage]
            [cdq.ui.stage]
            cdq.ui.dev-menu
            cdq.ui.action-bar
            cdq.ui.hp-mana-bar
            cdq.ui.windows.entity-info
            cdq.ui.windows.inventory
            cdq.ui.player-state-draw
            cdq.ui.message
            [cdq.ui.error-window :as error-window]
            [cdq.utils :as utils]
            [cdq.utils.tiled :as tiled]
            [cdq.ctx.world :as world]
            [clojure.math :as math]
            [clojure.string :as str]
            [qrecord.core :as q]))

(q/defrecord Context [ctx/config
                      ctx/input
                      ctx/db
                      ctx/audio
                      ctx/stage
                      ctx/mouseover-eid
                      ctx/player-eid
                      ctx/graphics
                      ctx/world])

(def ^:private schema
  (m/schema [:map {:closed true}
             [:ctx/config :some]
             [:ctx/input :some]
             [:ctx/db :some]
             [:ctx/audio :some]
             [:ctx/stage :some]
             [:ctx/mouseover-eid :any]
             [:ctx/player-eid :some]
             [:ctx/graphics :some]
             [:ctx/world :some]]))

(defn- validate [ctx]
  (m/validate-humanize schema ctx)
  ctx)

(defn- valid-tx? [transaction]
  (vector? transaction))

(defmulti do! (fn [[k & _params] _ctx]
                k))

(defn- handle-tx! [tx ctx]
  (assert (valid-tx? tx) (pr-str tx))
  (try
   (do! tx ctx)
   (catch Throwable t
     (throw (ex-info "Error handling transaction" {:transaction tx} t)))))

(defn handle-txs!
  "Handles transactions and returns a flat list of all transactions handled, including nested."
  [ctx transactions]
  (loop [ctx ctx
         txs transactions
         handled []]
    (if (seq txs)
      (let [tx (first txs)]
        (if tx
          (let [new-txs (handle-tx! tx ctx)]
              (recur ctx
                     (concat (or new-txs []) (rest txs))
                     (conj handled tx)))
          (recur ctx (rest txs) handled)))
      handled)))

(def ^:private k->colors {:property/pretty-name "PRETTY_NAME"
                          :entity/modifiers "CYAN"
                          :maxrange "LIGHT_GRAY"
                          :creature/level "GRAY"
                          :projectile/piercing? "LIME"
                          :skill/action-time-modifier-key "VIOLET"
                          :skill/action-time "GOLD"
                          :skill/cooldown "SKY"
                          :skill/cost "CYAN"
                          :entity/delete-after-duration "LIGHT_GRAY"
                          :entity/faction "SLATE"
                          :entity/fsm "YELLOW"
                          :entity/species "LIGHT_GRAY"
                          :entity/temp-modifier "LIGHT_GRAY"})

(def ^:private k-order [:property/pretty-name
                        :skill/action-time-modifier-key
                        :skill/action-time
                        :skill/cooldown
                        :skill/cost
                        :skill/effects
                        :entity/species
                        :creature/level
                        :creature/stats
                        :entity/delete-after-duration
                        :projectile/piercing?
                        :entity/projectile-collision
                        :maxrange
                        :entity-effects])

(defn- remove-newlines [s]
  (let [new-s (-> s
                  (str/replace "\n\n" "\n")
                  (str/replace #"^\n" "")
                  str/trim-newline)]
    (if (= (count new-s) (count s))
      s
      (remove-newlines new-s))))

(defmulti ^:private info-segment (fn [[k] _ctx]
                                   k))

(defmethod info-segment :default [_ _ctx])

(defn- info-text
  "Creates a formatted informational text representation of components."
  [ctx components]
  (->> components
       (utils/sort-by-k-order k-order)
       (keep (fn [{k 0 v 1 :as component}]
               (str (let [s (try (info-segment component ctx)
                                 (catch Throwable t
                                   ; fails for
                                   ; effects/spawn
                                   ; end entity/hp
                                   ; as already 'built' yet 'hp' not
                                   ; built from db yet ...
                                   (pr-str component)
                                   #_(throw (ex-info "info system failed"
                                                     {:component component}
                                                     t))))]
                      (if-let [color (k->colors k)]
                        (str "[" color "]" s "[]")
                        s))
                    (when (map? v)
                      (str "\n" (info-text ctx v))))))
       (str/join "\n")
       remove-newlines))

(defmulti ^:private op-value-text (fn [[k]]
                                    k))

(defmethod op-value-text :op/inc
  [[_ value]]
  (str value))

(defmethod op-value-text :op/mult
  [[_ value]]
  (str value "%"))

(defn- +? [n]
  (case (math/signum n)
    0.0 ""
    1.0 "+"
    -1.0 ""))

(defn- op-info [op k]
  (str/join "\n"
            (keep
             (fn [{v 1 :as component}]
               (when-not (zero? v)
                 (str (+? v) (op-value-text component) " " (str/capitalize (name k)))))
             (sort-by op/-order op))))

(defmethod info-segment :property/pretty-name [[_ v] _ctx]
  v)

(defmethod info-segment :maxrange [[_ v] _ctx]
  v)

(defmethod info-segment :creature/level [[_ v] _ctx]
  (str "Level: " v))

(defmethod info-segment :projectile/piercing?  [_ _ctx] ; TODO also when false ?!
  "Piercing")

(defmethod info-segment :skill/action-time-modifier-key [[_ v] _ctx]
  (case v
    :entity/cast-speed "Spell"
    :entity/attack-speed "Attack"))

(defmethod info-segment :skill/action-time [[_ v] _ctx]
  (str "Action-Time: " (utils/readable-number v) " seconds"))

(defmethod info-segment :skill/cooldown [[_ v] _ctx]
  (when-not (zero? v)
    (str "Cooldown: " (utils/readable-number v) " seconds")))

(defmethod info-segment :skill/cost [[_ v] _ctx]
  (when-not (zero? v)
    (str "Cost: " v " Mana")))

(def ^:private non-val-max-stat-ks
  [:entity/movement-speed
   :entity/aggro-range
   :entity/reaction-time
   :entity/strength
   :entity/cast-speed
   :entity/attack-speed
   :entity/armor-save
   :entity/armor-pierce])

(defmethod info-segment :creature/stats [[k stats] _ctx]
  (str/join "\n" (concat
                  ["*STATS*"
                   (str "Mana: " (if (:entity/mana stats)
                                   (modifiers/get-mana stats)
                                   "-"))
                   (str "Hitpoints: " (modifiers/get-hitpoints stats))]
                  (for [stat-k non-val-max-stat-ks]
                    (str (str/capitalize (name stat-k)) ": "
                         (modifiers/get-stat-value stats stat-k))))))

(defmethod info-segment :effects/spawn [[_ {:keys [property/pretty-name]}] _ctx]
  (str "Spawns a " pretty-name))

(defmethod info-segment :effects.target/convert [_ _ctx]
  "Converts target to your side.")

(defn- damage-info [{[min max] :damage/min-max}]
  (str min "-" max " damage"))

(defmethod info-segment :effects.target/damage [[_ damage] _ctx]
  (damage-info damage)
  #_(if source
      (let [modified (modifiers/damage @source damage)]
        (if (= damage modified)
          (damage-info damage)
          (str (damage-info damage) "\nModified: " (damage/info modified))))
      (damage-info damage)) ; property menu no source,modifiers
  )

(defmethod info-segment :effects.target/hp [[k ops] _ctx]
  (op-info ops k))

(defmethod info-segment :effects.target/kill [_ _ctx]
  "Kills target")

; FIXME no source
; => to entity move
(defmethod info-segment :effects.target/melee-damage [_ _ctx]
  (str "Damage based on entity strength."
       #_(when source
           (str "\n" (damage-info (entity->melee-damage @source))))))

(defmethod info-segment :effects.target/spiderweb [_ _ctx]
  "Spiderweb slows 50% for 5 seconds."
  ; modifiers same like item/modifiers has info-text
  ; counter ?
  )

(defmethod info-segment :effects.target/stun [[_ duration] _ctx]
  (str "Stuns for " (utils/readable-number duration) " seconds"))

(defmethod info-segment :effects/target-all [_ _ctx]
  "All visible targets")

(defmethod info-segment :entity/delete-after-duration [[_ counter] {:keys [ctx/world]}]
  (str "Remaining: " (utils/readable-number (timer/ratio (:world/elapsed-time world) counter)) "/1"))

(defmethod info-segment :entity/faction [[_ faction] _ctx]
  (str "Faction: " (name faction)))

(defmethod info-segment :entity/fsm [[_ fsm] _ctx]
  (str "State: " (name (:state fsm))))

; still used by item .. o.o
(defmethod info-segment :entity/modifiers [[_ mods] _ctx]
  (when (seq mods)
    (str/join "\n" (keep (fn [[k ops]]
                           (op-info ops k)) mods))))

(defmethod info-segment :entity/species [[_ species] _ctx]
  (str "Creature - " (str/capitalize (name species))))

(defmethod info-segment :entity/temp-modifier [[_ {:keys [counter]}] {:keys [ctx/world]}]
  (str "Spiderweb - remaining: " (utils/readable-number (timer/ratio (:world/elapsed-time world) counter)) "/1"))

; recursively printing all effects ... thaths why deactivated ...
; custom createure info foobaz?
(defmethod info-segment :entity/skills [[_ skills] _ctx]
  ; => recursive info-text leads to endless text wall
  (when (seq skills)
    (str "Skills: " (str/join "," (map name (keys skills))))))

(defn- add-skill!
  [{:keys [ctx/graphics
           ctx/stage]}
   skill]
  (stage/add-action-bar-skill! stage
                               {:skill-id (:property/id skill)
                                :texture-region (graphics/image->texture-region graphics (:entity/image skill))
                                ; (assoc ctx :effect/source (world/player)) FIXME
                                :tooltip-text #(info-text % skill)})
  nil)

(defn- remove-skill! [{:keys [ctx/stage]} skill]
  (stage/remove-action-bar-skill! stage (:property/id skill))
  nil)

(defn- set-item!
  [{:keys [ctx/graphics
           ctx/stage]
    :as ctx}
   inventory-cell item]
  (stage/set-inventory-item! stage
                             inventory-cell
                             {:texture-region (graphics/image->texture-region graphics (:entity/image item))
                              :tooltip-text (info-text ctx item)}))

(defn- remove-item! [{:keys [ctx/stage]} inventory-cell]
  (stage/remove-inventory-item! stage inventory-cell))

(defmethod do! :tx/assoc [[_ eid k value] _ctx]
  (swap! eid assoc k value)
  nil)

(defmethod do! :tx/assoc-in [[_ eid ks value] _ctx]
  (swap! eid assoc-in ks value)
  nil)

(defmethod do! :tx/dissoc [[_ eid k] _ctx]
  (swap! eid dissoc k)
  nil)

(defmethod do! :tx/mark-destroyed [[_ eid] _ctx]
  (swap! eid assoc :entity/destroyed? true)
  nil)

(defmethod do! :tx/mod-add [[_ eid modifiers] _ctx]
  (swap! eid entity/mod-add modifiers)
  nil)

(defmethod do! :tx/mod-remove [[_ eid modifiers] _ctx]
  (swap! eid entity/mod-remove modifiers)
  nil)

(defmethod do! :tx/effect [[_ effect-ctx effects] {:keys [ctx/world]}]
  (mapcat #(effect/handle % effect-ctx world)
          (effect/filter-applicable? effect-ctx effects)))

(defmethod do! :tx/event [[_ eid event params] {:keys [ctx/world]}]
  (fsm/event->txs world eid event params))

(defmethod do! :tx/add-skill [[_ eid skill] ctx]
  (swap! eid entity/add-skill skill)
  (when (:entity/player? @eid)
    (add-skill! ctx skill))
  nil)

#_(defn remove-skill [eid {:keys [property/id] :as skill}]
    {:pre [(contains? (:entity/skills @eid) id)]}
    (swap! eid update :entity/skills dissoc id)
    (when (:entity/player? @eid)
      (remove-skill! ctx skill)))

(defmethod do! :tx/set-cooldown [[_ eid skill] {:keys [ctx/world]}]
  (swap! eid assoc-in
         [:entity/skills (:property/id skill) :skill/cooling-down?]
         (timer/create (:world/elapsed-time world) (:skill/cooldown skill)))
  nil)

(defmethod do! :tx/add-text-effect [[_ eid text duration] {:keys [ctx/world]}]
  (swap! eid timers/add-text-effect text duration (:world/elapsed-time world))
  nil)

(defmethod do! :tx/pay-mana-cost [[_ eid cost] _ctx]
  (swap! eid entity/pay-mana-cost cost)
  nil)

(defmethod do! :tx/set-item [[_ eid cell item] ctx]
  (let [entity @eid
        inventory (:entity/inventory entity)]
    (assert (and (nil? (get-in inventory cell))
                 (inventory/valid-slot? cell item)))
    (swap! eid assoc-in (cons :entity/inventory cell) item)
    (when (inventory/applies-modifiers? cell)
      (swap! eid entity/mod-add (:entity/modifiers item)))
    (when (:entity/player? entity)
      (set-item! ctx cell item))
    nil))

(defmethod do! :tx/pickup-item [[_ eid item] ctx]
  (let [[cell cell-item] (inventory/can-pickup-item? (:entity/inventory @eid) item)]
    (assert cell)
    (assert (or (inventory/stackable? item cell-item)
                (nil? cell-item)))
    (if (inventory/stackable? item cell-item)
      (do
       #_(tx/stack-item ctx eid cell item))
      (do! [:tx/set-item eid cell item] ctx))))

(defmethod do! :tx/remove-item [[_ eid cell] ctx]
  (let [entity @eid
        item (get-in (:entity/inventory entity) cell)]
    (assert item)
    (swap! eid assoc-in (cons :entity/inventory cell) nil)
    (when (inventory/applies-modifiers? cell)
      (swap! eid entity/mod-remove (:entity/modifiers item)))
    (when (:entity/player? entity)
      (remove-item! ctx cell))
    nil))

; TODO doesnt exist, stackable, usable items with action/skillbar thingy
#_(defn remove-one-item [eid cell]
  (let [item (get-in (:entity/inventory @eid) cell)]
    (if (and (:count item)
             (> (:count item) 1))
      (do
       ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
       ; first remove and then place, just update directly  item ...
       (cdq.tx.remove-item/do! eid cell)
       (cdq.tx.set-item/do! eid cell (update item :count dec)))
      (cdq.tx.remove-item/do! eid cell))))

; TODO no items which stack are available
#_(defn stack-item [eid cell item]
  (let [cell-item (get-in (:entity/inventory @eid) cell)]
    (assert (inventory/stackable? item cell-item))
    ; TODO this doesnt make sense with modifiers ! (triggered 2 times if available)
    ; first remove and then place, just update directly  item ...
    (concat (cdq.tx.remove-item/do! eid cell)
            (cdq.tx.set-item/do! eid cell (update cell-item :count + (:count item))))))

#_(defn do! [ctx eid cell item]
  #_(tx/stack-item ctx eid cell item))

(declare ^:private reset-game-state!)

(def ^:private state->clicked-inventory-cell
  {:player-idle           cdq.entity.state.player-idle/clicked-inventory-cell
   :player-item-on-cursor cdq.entity.state.player-item-on-cursor/clicked-cell})

(comment

 ; items then have 2x pretty-name
 #_(.setText (.getTitleLabel window)
             (info-text [:property/pretty-name (:property/pretty-name entity)])
             "Entity Info")
 )

(def disallowed-keys [:entity/skills
                      #_:entity/fsm
                      :entity/faction
                      :active-skill])

; TODO details how the text looks move to info
; only for :
; * skill
; * entity -> all sub-types
; * item
; => can test separately !?

(defn- ->label-text [entity ctx]
  ; don't use select-keys as it loses Entity record type
  (info-text ctx (apply dissoc entity disallowed-keys)))

(defn- create-ui-actors [ctx]
  [(cdq.ui.dev-menu/create ctx ; graphics db
                           {:reset-game-state-fn reset-game-state!
                            :world-fns [['cdq.level.from-tmx/create
                                         {:tmx-file "maps/vampire.tmx"
                                          :start-position [32 71]}]
                                        ['cdq.level.uf-caves/create
                                         {:tile-size 48
                                          :texture "maps/uf_terrain.png"
                                          :spawn-rate 0.02
                                          :scaling 3
                                          :cave-size 200
                                          :cave-style :wide}]
                                        ['cdq.level.modules/create
                                         {:world/map-size 5,
                                          :world/max-area-level 3,
                                          :world/spawn-rate 0.05}]]
                            ;icons, etc. , components ....
                            :info "[W][A][S][D] - Move\n[I] - Inventory window\n[E] - Entity Info window\n[-]/[=] - Zoom\n[P]/[SPACE] - Unpause"})
    (cdq.ui.action-bar/create {:id :action-bar}) ; padding.... !, etc.

    ; graphics
    (cdq.ui.hp-mana-bar/create ctx
                               {:rahmen-file "images/rahmen.png"
                                :rahmenw 150
                                :rahmenh 26
                                :hpcontent-file "images/hp.png"
                                :manacontent-file "images/mana.png"
                                :y-mana 80}) ; action-bar-icon-size

    {:actor/type :actor.type/group
     :id :windows
     :actors [(cdq.ui.windows.entity-info/create ctx {:y 0
                                                      :->label-text ->label-text
                                                      }) ; graphics only
              (cdq.ui.windows.inventory/create ctx ; graphics only
               {:title "Inventory"
                :id :inventory-window
                :visible? false
                :clicked-cell-fn (fn [cell]
                                   (fn [{:keys [ctx/player-eid] :as ctx}]
                                     (handle-txs!
                                      ctx
                                      (when-let [f (state->clicked-inventory-cell (:state (:entity/fsm @player-eid)))]
                                        (f player-eid cell)))))})]}
    (cdq.ui.player-state-draw/create
     {:state->draw-gui-view
      {:player-item-on-cursor
       cdq.entity.state.player-item-on-cursor/draw-gui-view}})
    (cdq.ui.message/create {:duration-seconds 0.5
                            :name "player-message"})])

; alternative: add actors at beginning
; and call 'reset-state!' function on each actor
(defn- reset-stage!
  [{:keys [ctx/stage]
    :as ctx}]
  (cdq.ui.stage/clear! stage)
  (doseq [actor (create-ui-actors ctx)]
    (cdq.ui.stage/add! stage actor))
  ctx)

(defn- add-ctx-world
  [{:keys [ctx/config]
    :as ctx}
   world-fn]
  (assoc ctx :ctx/world (world/create (merge (::world config)
                                             (let [[f params] world-fn]
                                               ((requiring-resolve f) ctx params))))))

(defn- spawn-player!
  [{:keys [ctx/config
           ctx/db
           ctx/world]
    :as ctx}]
  (->> (let [{:keys [creature-id
                     components]} (:cdq.ctx.game/player-props config)]
         {:position (utils/tile->middle (:world/start-position world))
          :creature-property (db/build db creature-id)
          :components components})
       (world/spawn-creature! world)
       (handle-txs! ctx))
  (let [player-eid (get @(:world/entity-ids world) 1)]
    (assert (:entity/player? @player-eid))
    (assoc ctx :ctx/player-eid player-eid)))

(defn- spawn-enemies!
  [{:keys [ctx/config
           ctx/db
           ctx/world]
    :as ctx}]
  (doseq [[position creature-id] (tiled/positions-with-property (:world/tiled-map world)
                                                                "creatures"
                                                                "id")]
    (->> {:position (utils/tile->middle position)
          :creature-property (db/build db (keyword creature-id))
          :components (:cdq.ctx.game/enemy-components config)}
         (world/spawn-creature! world)
         (handle-txs! ctx)))
  ctx)

; TODO dispose old tiled-map if already ctx/world present - or call 'dispose!'
(defn- reset-game-state! [ctx world-fn]
  (-> ctx
      reset-stage!  ; TODO work here with cdq.ui as stage ctx/stage not libgdx scene2d stage ....
      (add-ctx-world world-fn)
      spawn-player!
      spawn-enemies!))

(defn- check-open-debug-data-view!
  [{:keys [ctx/input
           ctx/stage
           ctx/mouseover-eid
           ctx/world
           ctx/world-mouse-position]
    :as ctx}]
  (when (input/button-just-pressed? input :right)
    (let [data (or (and mouseover-eid @mouseover-eid)
                   @(grid/cell (:world/grid world)
                               (mapv int world-mouse-position)))]
      (cdq.ui.stage/add! stage
                         (data-view/table-view-window {:title "Data View"
                                                       :data data
                                                       :width 500
                                                       :height 500}))))
  ctx)

(defn- assoc-active-entities [ctx]
  (update ctx :ctx/world world/cache-active-entities @(:ctx/player-eid ctx)))

(defn- set-camera-on-player!
  [{:keys [ctx/graphics
           ctx/player-eid]
    :as ctx}]
  (camera/set-position! (:viewport/camera (:world-viewport graphics))
                        (entity/position @player-eid))
  ctx)

(defn- clear-screen!
  [{:keys [ctx/graphics] :as ctx}]
  (graphics/clear-screen! graphics :black)
  ctx)

(defn- draw-world-map!
  [{:keys [ctx/graphics
           ctx/world]
    :as ctx}]
  (graphics/draw-tiled-map! graphics
                            (:world/tiled-map world)
                            (tile-color-setter/create
                             {:ray-blocked? (let [raycaster (:world/raycaster world)]
                                              (fn [start end] (raycaster/blocked? raycaster start end)))
                              :explored-tile-corners (:world/explored-tile-corners world)
                              :light-position (:camera/position (:viewport/camera (:world-viewport graphics)))
                              :see-all-tiles? false
                              :explored-tile-color  [0.5 0.5 0.5 1]
                              :visible-tile-color   [1 1 1 1]
                              :invisible-tile-color [0 0 0 1]}))
  ctx)

(def ^:dbg-flag show-tile-grid? false)
(def ^:dbg-flag show-potential-field-colors? false) ; :good, :evil
(def ^:dbg-flag show-cell-entities? false)
(def ^:dbg-flag show-cell-occupied? false)

(defn- draw-tile-grid* [world-viewport]
  (let [[left-x _right-x bottom-y _top-y] (camera/frustum (:viewport/camera world-viewport))]
    [[:draw/grid
      (int left-x)
      (int bottom-y)
      (inc (int (:viewport/width world-viewport)))
      (+ 2 (int (:viewport/height world-viewport)))
      1
      1
      [1 1 1 0.8]]]))

(defn- draw-tile-grid [{:keys [ctx/graphics] :as ctx}]
  (when show-tile-grid?
    (graphics/handle-draws! graphics (draw-tile-grid* (:world-viewport graphics)))))

(defn- draw-cell-debug* [{:keys [ctx/world
                                 ctx/graphics]}]
  (let [grid (:world/grid world)]
    (apply concat
           (for [[x y] (camera/visible-tiles (:viewport/camera (:world-viewport graphics)))
                 :let [cell (grid/cell grid [x y])]
                 :when cell
                 :let [cell* @cell]]
             [(when (and show-cell-entities? (seq (:entities cell*)))
                [:draw/filled-rectangle x y 1 1 [1 0 0 0.6]])
              (when (and show-cell-occupied? (seq (:occupied cell*)))
                [:draw/filled-rectangle x y 1 1 [0 0 1 0.6]])
              (when-let [faction show-potential-field-colors?]
                (let [{:keys [distance]} (faction cell*)]
                  (when distance
                    (let [ratio (/ distance ((:world/factions-iterations world) faction))]
                      [:draw/filled-rectangle x y 1 1 [ratio (- 1 ratio) ratio 0.6]]))))]))))

(defn- draw-cell-debug [{:keys [ctx/graphics] :as ctx}]
  (graphics/handle-draws! graphics (draw-cell-debug* ctx)))

(def ^:dbg-flag show-body-bounds? false)

(defn- draw-body-rect [{:keys [body/position body/width body/height]} color]
  (let [[x y] [(- (position 0) (/ width  2))
               (- (position 1) (/ height 2))]]
    [[:draw/rectangle x y width height color]]))

(defn- draw-entity [{:keys [ctx/graphics] :as ctx} entity render-layer]
  (try
   (when show-body-bounds?
     (graphics/handle-draws! graphics (draw-body-rect (:entity/body entity) (if (:body/collides? (:entity/body entity)) :white :gray))))
   ; not doseq k v but doseq render-layer-components ...
   (doseq [[k v] entity
           :let [draw-fn (get render-layer k)]
           :when draw-fn]
     (graphics/handle-draws! graphics (draw-fn v entity ctx)))
   (catch Throwable t
     (graphics/handle-draws! graphics (draw-body-rect (:entity/body entity) :red))
     (stacktrace/pretty-print t))))

(defn- render-entities
  [{:keys [ctx/player-eid
           ctx/world]
    :as ctx}]
  (let [entities (map deref (:world/active-entities world))
        player @player-eid
        should-draw? (fn [entity z-order]
                       (or (= z-order :z-order/effect)
                           (world/line-of-sight? world player entity)))]
    (doseq [[z-order entities] (utils/sort-by-order (group-by (comp :body/z-order :entity/body) entities)
                                                    first
                                                    (:world/render-z-order world))
            render-layer cdq.entity-api/render-layers
            entity entities
            :when (should-draw? entity z-order)]
      (draw-entity ctx entity render-layer))))

(defn- geom-test*
  [{:keys [ctx/world
           ctx/world-mouse-position]}]
  (let [grid (:world/grid world)
        position world-mouse-position
        radius 0.8
        circle {:position position
                :radius radius}]
    (conj (cons [:draw/circle position radius [1 0 0 0.5]]
                (for [[x y] (map #(:position @%) (grid/circle->cells grid circle))]
                  [:draw/rectangle x y 1 1 [1 0 0 0.5]]))
          (let [{:keys [x y width height]} (geom/circle->outer-rectangle circle)]
            [:draw/rectangle x y width height [0 0 1 1]]))))

(defn- geom-test [{:keys [ctx/graphics] :as ctx}]
  (graphics/handle-draws! graphics (geom-test* ctx)))

(defn- highlight-mouseover-tile
  [{:keys [ctx/graphics
           ctx/world
           ctx/world-mouse-position]}]
  (graphics/handle-draws! graphics
                          (let [[x y] (mapv int world-mouse-position)
                                cell (grid/cell (:world/grid world) [x y])]
                            (when (and cell (#{:air :none} (:movement @cell)))
                              [[:draw/rectangle x y 1 1
                                (case (:movement @cell)
                                  :air  [1 1 0 0.5]
                                  :none [1 0 0 0.5])]]))))

(defn- draw-on-world-viewport!
  [{:keys [ctx/graphics] :as ctx}]
  (graphics/draw-on-world-viewport! graphics
                                    (fn []
                                      (doseq [f [draw-tile-grid
                                                 draw-cell-debug
                                                 render-entities
                                                 ; geom-test
                                                 highlight-mouseover-tile]]
                                        (f ctx))))
  ctx)

(defn- render-stage! [{:keys [ctx/stage] :as ctx}]
  (cdq.ui.stage/render! stage ctx))

(def ^:private state->cursor
  {:active-skill :cursors/sandclock
   :player-dead :cursors/black-x
   :player-idle cdq.interaction-state/->cursor
   :player-item-on-cursor :cursors/hand-grab
   :player-moving :cursors/walking
   :stunned :cursors/denied})

(defn- set-cursor!
  [{:keys [ctx/graphics
           ctx/player-eid]
    :as ctx}]
  ; world/player-state
  (graphics/set-cursor! graphics (let [->cursor (state->cursor (:state (:entity/fsm @player-eid)))]
                                   (if (keyword? ->cursor)
                                     ->cursor
                                     (->cursor player-eid ctx))))
  ctx)

(def ^:private state->handle-input
  {:player-idle           cdq.entity.state.player-idle/handle-input
   :player-item-on-cursor cdq.entity.state.player-item-on-cursor/handle-input
   :player-moving         cdq.entity.state.player-moving/handle-input})

(defn- player-state-handle-input!
  [{:keys [ctx/player-eid]
    :as ctx}]
  (let [handle-input (state->handle-input (:state (:entity/fsm @player-eid)))
        txs (if handle-input
              (handle-input player-eid ctx)
              nil)]
    (handle-txs! ctx txs))
  ctx)

(defn- update-mouseover-entity!
  [{:keys [ctx/mouseover-actor
           ctx/mouseover-eid
           ctx/player-eid
           ctx/world
           ctx/world-mouse-position]
    :as ctx}]
  (let [new-eid (if mouseover-actor
                  nil
                  (let [player @player-eid
                        hits (remove #(= (:body/z-order (:entity/body @%)) :z-order/effect)
                                     (grid/point->entities (:world/grid world) world-mouse-position))]
                    (->> (:world/render-z-order world)
                         (utils/sort-by-order hits #(:body/z-order (:entity/body @%)))
                         reverse
                         (filter #(world/line-of-sight? world player @%))
                         first)))]
    (when mouseover-eid
      (swap! mouseover-eid dissoc :entity/mouseover?))
    (when new-eid
      (swap! new-eid assoc :entity/mouseover? true))
    (assoc ctx :ctx/mouseover-eid new-eid)))

(def ^:private pausing? true)

(def ^:private state->pause-game?
  {:stunned false
   :player-moving false
   :player-item-on-cursor true
   :player-idle true
   :player-dead true
   :active-skill false})

(defn- assoc-paused
  [{:keys [ctx/input
           ctx/player-eid
           ctx/config]
    :as ctx}]
  (assoc-in ctx [:ctx/world :world/paused?]
            (let [controls (:controls config)]
              (or #_error
                  (and pausing?
                       (state->pause-game? (:state (:entity/fsm @player-eid)))
                       (not (or (input/key-just-pressed? input (:unpause-once controls))
                                (input/key-pressed?      input (:unpause-continously controls)))))))))

(defn- update-time [{:keys [ctx/graphics
                            ctx/world]
                     :as ctx}]
  (update ctx :ctx/world world/update-time (graphics/delta-time graphics)))

(defn- update-potential-fields!
  [{:keys [ctx/world]
    :as ctx}]
  (world/tick-potential-fields! world)
  ctx)

(defn- tick-entity! [{:keys [ctx/world] :as ctx} eid]
  (doseq [k (keys @eid)]
    (try (when-let [v (k @eid)]
           (handle-txs! ctx (when-let [f (cdq.entity-api/entity->tick k)]
                              (f v eid world))))
         (catch Throwable t
           (throw (ex-info "entity-tick"
                           {:k k
                            :entity/id (entity/id @eid)}
                           t))))))
(defn- tick-entities!
  [{:keys [ctx/stage
           ctx/world]
    :as ctx}]
  (try
   (doseq [eid (:world/active-entities world)]
     (tick-entity! ctx eid))
   (catch Throwable t
     (stacktrace/pretty-print t)
     (cdq.ui.stage/add! stage (error-window/create t))
     #_(bind-root ::error t)))
  ctx)

(defn- tick-world!
  [ctx]
  (if (get-in ctx [:ctx/world :world/paused?])
    ctx
    (-> ctx
        update-time
        update-potential-fields!
        tick-entities!)))

(defn- remove-destroyed-entities!
  [{:keys [ctx/world]
    :as ctx}]
  (doseq [eid (filter (comp :entity/destroyed? deref)
                      (vals @(:world/entity-ids world)))]
    (handle-txs! ctx (world/remove-entity! world eid)))
  ctx)

(def ^:private zoom-speed 0.025)

(defn- check-camera-controls!
  [{:keys [ctx/config
           ctx/input
           ctx/graphics]
    :as ctx}]
  (let [controls (:controls config)]
    (when (input/key-pressed? input (:zoom-in  controls)) (graphics/zoom-in!  graphics zoom-speed))
    (when (input/key-pressed? input (:zoom-out controls)) (graphics/zoom-out! graphics zoom-speed)))
  ctx)

(def ^:private close-windows-key  :escape)
(def ^:private toggle-inventory   :i)
(def ^:private toggle-entity-info :e)

(defn- check-window-hotkeys!
  [{:keys [ctx/input
           ctx/stage]
    :as ctx}]
  (when (input/key-just-pressed? input close-windows-key)  (stage/close-all-windows!         stage))
  (when (input/key-just-pressed? input toggle-inventory )  (stage/toggle-inventory-visible!  stage))
  (when (input/key-just-pressed? input toggle-entity-info) (stage/toggle-entity-info-window! stage))
  ctx)

(defn create! [gdx config]
  (let [input (:input gdx)
        graphics (graphics/create! gdx (::graphics config))
        stage (stage/create! graphics (::stage config))]
    (input/set-processor! input stage)
    (-> (map->Context {:audio (audio/create gdx (::audio config))
                       :config (::config config)
                       :db (db/create (::db config))
                       :graphics graphics
                       :input input
                       :stage stage})
        (reset-game-state! (::starting-level config))
        (assoc :ctx/mouseover-eid nil)
        validate)))

(defn dispose! [{:keys [ctx/audio
                        ctx/graphics
                        ctx/world]}]
  (audio/dispose! audio)
  (graphics/dispose! graphics)
  (world/dispose! world)
  (stage/dispose!))

(defn- assoc-mouseover-keys
  [{:keys [ctx/graphics
           ctx/input
           ctx/stage]
    :as ctx}]
  (let [mouse-position (input/mouse-position input)
        ui-mouse-position    (graphics/unproject-ui    graphics mouse-position)
        world-mouse-position (graphics/unproject-world graphics mouse-position)]
    (assoc ctx
           :ctx/mouseover-actor      (stage/hit stage ui-mouse-position)
           :ctx/ui-mouse-position    ui-mouse-position
           :ctx/world-mouse-position world-mouse-position)))

(defn- dissoc-mouseover-keys
  [ctx]
  (dissoc ctx
          :ctx/mouseover-actor
          :ctx/ui-mouse-position
          :ctx/world-mouse-position))

(defn render! [ctx]
  (-> ctx
      validate
      assoc-mouseover-keys
      update-mouseover-entity!
      check-open-debug-data-view! ; TODO FIXME its not documented I forgot rightclick can open debug data view!
      assoc-active-entities
      set-camera-on-player!
      clear-screen!
      draw-world-map!
      draw-on-world-viewport!
      render-stage!
      set-cursor!
      player-state-handle-input!
      assoc-paused
      tick-world!
      remove-destroyed-entities! ; do not pause as pickup item should be destroyed
      check-camera-controls!
      check-window-hotkeys!
      dissoc-mouseover-keys
      validate))

(defn resize! [{:keys [ctx/graphics]} width height]
  (graphics/resize-viewports! graphics width height))
