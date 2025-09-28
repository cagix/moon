(ns cdq.application
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            [gdl.application :as application])
  (:gen-class))

(def state (atom nil))

(defn -main []
  (let [config (-> "cdq.application.edn" io/resource slurp edn/read-string)
        create-pipeline (map requiring-resolve (:create config))
        dispose (requiring-resolve (:dispose config))
        render-pipeline (map requiring-resolve (:render config))
        resize (requiring-resolve (:resize config))]
    (application/start!
     {:listener (reify application/Listener
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
                  (resume [_]))
      :config config})))
