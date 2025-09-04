(ns cdq.dev
  (:require [cdq.application :as application]
            [cdq.ctx :as ctx]
            [cdq.db :as db]
            [cdq.dev.app-values]
            [cdq.entity :as entity]))

(defn post-runnable! [f]
  (.postRunnable com.badlogic.gdx.Gdx/app
                 (fn [] (f @application/state))))

(comment

 ; fsm missing
 (cdq.dev.app-values/print-app-values-tree "app-values-tree.clj"
                                           #{"cdq" "gdx"})

 (cdq.dev.app-values/ns-value-vars 'cdq.entity.fsm)

 (clojure.pprint/pprint
  (for [[sym var] (ns-interns 'cdq.entity.fsm)]
    [sym {:class (class @var)
          :supers (supers (class @var))}]))

 (clojure.pprint/pprint
  (sort (keys @cdq.application/state)))

 (post-txs!
  [[:tx/show-modal {:title "hey title"
                    :text "my text"
                    :button-text "button txt"
                    :on-click (fn []
                                (println "hoho"))}]])


 ; use post-runnable! to get proper error messages in console

 (show-tree-view! "Mouseover Entity" ctx/mouseover-eid)
 (show-tree-view! "Mouseover Grid Cell" (mouseover-grid-cell))
 (show-tree-view! "Ns vaue Vars" (ns-value-vars #{"clojure"}))

 (spit "ns_value_vars.edn"
       (with-out-str
        (clojure.pprint/pprint
         (ns-value-vars #{"clojure"}))))

 ; Idea:
 ; * Generate the tree as data-structure first
 ; Then pass to the ui
 ; So can test it locally?
 ; or some interface for tree-node & on-clicked


 (do
  (learn-skill! :skills/projectile)
  (learn-skill! :skills/spawn)
  (learn-skill! :skills/meditation)
  (learn-skill! :skills/death-ray)
  (learn-skill! :skills/convert)
  (learn-skill! :skills/blood-curse)
  (learn-skill! :skills/slow)
  (learn-skill! :skills/bow)
  (learn-skill! :skills/double-fireball))

 ; Testing effect/target-entity
 ; stops when target not exists anymore
 ; or out of range

 ; 1. start application
 ; 2. start world
 ; 3. create creature
 (post-runnable!
  (fn [ctx]
    (ctx/handle-txs! ctx
                     [[:tx/spawn-creature
                       {:position [35 73]
                        :creature-property (db/build (:ctx/db ctx) :creatures/dragon-red)
                        :components {:entity/fsm {:fsm :fsms/npc
                                                  :initial-state :npc-sleeping}
                                     :entity/faction :evil}}]])))

 (learn-skill! :skills/bow) ; 1.5 seconds attacktime
 (post-tx! [:e/destroy (ids->eids 168)]) ; TODO how to get id ?
 ; check state is normal ... omg...

 ; start world - small empty test room
 ; 2 creatures - player?
 ; start skill w. applicable needs target (bow)
 ; this command:
 ; check skill has stopped using
 ; => this is basically a test for 'forge.entity.active'

 )


#_(defn- learn-skill! [_context skill-id]
  (clojure.tx.add-skill/do! ctx/player-eid (db/build db skill-id)))

#_(defn- create-item! [_context item-id]
  (clojure.tx.spawn-item/do! (entity/position @ctx/player-eid) (db/build db item-id)))
