(ns cdq.dev
  (:require [cdq.ctx :as ctx]
            [cdq.entity :as entity]
            [cdq.g :as g]
            [clojure.string :as str]
            [clojure.pprint :refer [pprint]])
  (:import (com.badlogic.gdx Gdx)))

(defmacro post-runnable! [& exprs]
  `(.postRunnable Gdx/app (fn [] ~@exprs)))

#_(defn post-txs! [txs]
  (post-runnable!
   (g/handle-txs! #_@app/state txs)))

(comment

 (post-txs!
  [[:tx/show-modal {:title "hey title"
                    :text "my text"
                    :button-text "button txt"
                    :on-click (fn []
                                (println "hoho"))}]])

 (print-app-values-tree "app-values-tree.clj" #{"cdq"})

 ; use post-runnable! to get proper error messages in console

 (show-tree-view! "Mouseover Entity" ctx/mouseover-eid)
 (show-tree-view! "Mouseover Grid Cell" (mouseover-grid-cell))
 (show-tree-view! "Ns vaue Vars" (ns-value-vars #{"cdq"}))

 (spit "ns_value_vars.edn"
       (with-out-str
        (clojure.pprint/pprint
         (ns-value-vars #{"cdq"}))))

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
  [[:tx/spawn-creature {:position [35 73]
                        :creature-id :creatures/dragon-red
                        :components {:entity/fsm {:fsm :fsms/npc
                                                  :initial-state :npc-sleeping}
                                     :entity/faction :evil}}]])

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
  (cdq.tx.add-skill/do! ctx/player-eid (db/build db skill-id ctx)))

#_(defn- create-item! [_context item-id]
  (cdq.tx.spawn-item/do! (entity/position @ctx/player-eid) (db/build db item-id ctx)))

(defn- mouseover-grid-cell [{:keys [ctx/grid] :as ctx}]
  #_@(grid/cell grid (mapv int (g/world-mouse-position ctx))))

(defn get-namespaces [packages]
  (filter #(packages (first (str/split (name (ns-name %)) #"\.")))
          (all-ns)))

(defn get-vars [nmspace condition]
  (for [[sym avar] (ns-interns nmspace)
       :when (condition avar)]
    avar))

(defn- protocol? [value]
  (and (instance? clojure.lang.PersistentArrayMap value)
       (:on value)))

(defn- get-non-fn-vars [nmspace]
  (get-vars nmspace (fn [avar]
                      (let [value @avar]
                        (not (or (fn? value)
                                 (instance? clojure.lang.MultiFn value)
                                 (protocol? value)
                                 ; anonymous class (proxy)
                                 (instance? java.lang.Class value)))))))

(defn ns-value-vars
  "Returns a map of ns-name to value-vars (non-function vars).
  Use to understand the state of your application.

  Example: `(ns-value-vars #{\"forge\"})`"
  [packages]
  (into {} (for [nmspace (get-namespaces packages)
                 :let [value-vars (get-non-fn-vars nmspace)]
                 :when (seq value-vars)]
             [(ns-name nmspace) value-vars])))

(defn print-app-values-tree [file namespaces-set]
  (spit file
        (with-out-str
         (pprint
          (for [[ns-name vars] (ns-value-vars namespaces-set)]
            [ns-name (map #(:name (meta %)) vars)])))))
