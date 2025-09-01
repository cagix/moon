(ns cdq.game.reset-game-state
  (:require [cdq.ctx :as ctx]
            [cdq.ctx.db :as db]
            [cdq.world.effect :as effect]
            [cdq.world.entity :as entity]
            [cdq.entity.fsm :as fsm]
            [cdq.entity.timers :as timers]
            [cdq.ctx.graphics :as graphics]
            [cdq.inventory :as inventory]
            [cdq.world.entity.stats :as modifiers]
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
            [cdq.utils :as utils]
            [cdq.utils.tiled :as tiled]
            [cdq.ctx.world :as world]
            [clojure.math :as math]
            [clojure.string :as str]))

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

(defmethod ctx/do! :tx/assoc [[_ eid k value] _ctx]
  (swap! eid assoc k value)
  nil)

(defmethod ctx/do! :tx/assoc-in [[_ eid ks value] _ctx]
  (swap! eid assoc-in ks value)
  nil)

(defmethod ctx/do! :tx/dissoc [[_ eid k] _ctx]
  (swap! eid dissoc k)
  nil)

(defmethod ctx/do! :tx/mark-destroyed [[_ eid] _ctx]
  (swap! eid assoc :entity/destroyed? true)
  nil)

(defmethod ctx/do! :tx/mod-add [[_ eid modifiers] _ctx]
  (swap! eid entity/mod-add modifiers)
  nil)

(defmethod ctx/do! :tx/mod-remove [[_ eid modifiers] _ctx]
  (swap! eid entity/mod-remove modifiers)
  nil)

(defmethod ctx/do! :tx/effect [[_ effect-ctx effects] {:keys [ctx/world]}]
  (mapcat #(effect/handle % effect-ctx world)
          (effect/filter-applicable? effect-ctx effects)))

(defmethod ctx/do! :tx/event [[_ eid event params] {:keys [ctx/world]}]
  (fsm/event->txs world eid event params))

(defmethod ctx/do! :tx/add-skill [[_ eid skill] ctx]
  (swap! eid entity/add-skill skill)
  (when (:entity/player? @eid)
    (add-skill! ctx skill))
  nil)

#_(defn remove-skill [eid {:keys [property/id] :as skill}]
    {:pre [(contains? (:entity/skills @eid) id)]}
    (swap! eid update :entity/skills dissoc id)
    (when (:entity/player? @eid)
      (remove-skill! ctx skill)))

(defmethod ctx/do! :tx/set-cooldown [[_ eid skill] {:keys [ctx/world]}]
  (swap! eid assoc-in
         [:entity/skills (:property/id skill) :skill/cooling-down?]
         (timer/create (:world/elapsed-time world) (:skill/cooldown skill)))
  nil)

(defmethod ctx/do! :tx/add-text-effect [[_ eid text duration] {:keys [ctx/world]}]
  (swap! eid timers/add-text-effect text duration (:world/elapsed-time world))
  nil)

(defmethod ctx/do! :tx/pay-mana-cost [[_ eid cost] _ctx]
  (swap! eid entity/pay-mana-cost cost)
  nil)

(defmethod ctx/do! :tx/set-item [[_ eid cell item] ctx]
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

(defmethod ctx/do! :tx/pickup-item [[_ eid item] ctx]
  (let [[cell cell-item] (inventory/can-pickup-item? (:entity/inventory @eid) item)]
    (assert cell)
    (assert (or (inventory/stackable? item cell-item)
                (nil? cell-item)))
    (if (inventory/stackable? item cell-item)
      (do
       #_(tx/stack-item ctx eid cell item))
      (ctx/do! [:tx/set-item eid cell item] ctx))))

(defmethod ctx/do! :tx/remove-item [[_ eid cell] ctx]
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

#_(defn ctx/do! [ctx eid cell item]
  #_(tx/stack-item ctx eid cell item))


(declare ^:private reset-game-state!)

(def state->clicked-inventory-cell)

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

(def state->draw-gui-view)

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
                                     (ctx/handle-txs!
                                      ctx
                                      (when-let [f (state->clicked-inventory-cell (:state (:entity/fsm @player-eid)))]
                                        (f player-eid cell)))))})]}
    (cdq.ui.player-state-draw/create state->draw-gui-view)
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
       (ctx/handle-txs! ctx))
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
         (ctx/handle-txs! ctx)))
  ctx)

; TODO dispose old tiled-map if already ctx/world present - or call 'dispose!'
(defn do! [ctx world-fn]
  (-> ctx
      reset-stage!
      (add-ctx-world world-fn)
      spawn-player!
      spawn-enemies!))

(def ^:private reset-game-state! do!)
