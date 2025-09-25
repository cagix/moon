(ns cdq.application
  (:require [gdl.application :as application]
            [clojure.java.io :as io]
            [clojure.edn :as edn]
            [com.badlogic.gdx.backends.lwjgl3.application :as lwjgl3-application])
  (:gen-class))

(def ^:private state (atom nil))

(defn -main []
  (let [{:keys [starter
                listener
                config]} (-> "cdq.application.edn"
                             io/resource
                             slurp
                             edn/read-string)]
    (lwjgl3-application/start!
     {:listener (let [{:keys [create
                              dispose
                              render
                              resize]} listener
                      create-pipeline (map requiring-resolve create)
                      dispose (requiring-resolve dispose)
                      render-pipeline (map requiring-resolve render)
                      resize (requiring-resolve resize)]
                  (reify lwjgl3-application/Listener
                    (create [_ context]
                      (reset! state (reduce (fn [ctx f]
                                              (f ctx))
                                            context
                                            create-pipeline)))
                    (dispose [_]
                      (dispose @state))
                    (pause [_])
                    (render [_]
                      (swap! state (fn [ctx]
                                     (reduce (fn [ctx f]
                                               (f ctx))
                                             ctx
                                             render-pipeline))))
                    (resize [_ width height]
                      (resize @state width height))
                    (resume [_])))
      :config config})))

(defn post-runnable! [f]
  (application/post-runnable! (:ctx/app @state)
                              (fn [] (f @state))))

(comment
 (require '[cdq.ctx :as ctx]
          '[cdq.db :as db])

 (post-runnable!
  (fn [ctx]
    (ctx/handle-txs! ctx
                     [[:tx/spawn-creature
                       {:position [35 73]
                        :creature-property (db/build (:ctx/db ctx) :creatures/dragon-red)
                        :components {:entity/fsm {:fsm :fsms/npc
                                                  :initial-state :npc-sleeping}
                                     :entity/faction :evil}}]])))
 )
