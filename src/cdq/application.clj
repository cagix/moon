(ns cdq.application
  (:require [clojure.edn :as edn]
            [clojure.java.io :as io]
            cdq.application.desktop)
  (:import (com.badlogic.gdx ApplicationAdapter Gdx)
           (com.badlogic.gdx.utils Disposable)
           (com.badlogic.gdx.utils.viewport Viewport)))

(def state
  "Do not call `swap!`, instead use `post-runnable!`, as the main game loop has side-effects and should not be retried.

  (Should probably make this private and have a `get-state` function)"
  (atom nil))

(defn -main
  "Reads `cdq.application.edn` as `config`.

  * `:requires` - a sequence of namespaces to require.

  * `:create-pipeline` - a vector or `(fn [context config] context)` for creating the initial context.

  * `:render-pipeline` - a vector or `(fn [context] context)` for the main loop.

  Starts a libgdx desktop application using the lwjgl3-backend, `reset!` and `swap!`-ing the `state` atom on callbacks.

  TODO:
  On `resize` callback -> updates viewports."
  []
  (let [config (-> "cdq.application.edn" io/resource slurp edn/read-string)
        create-pipeline (map requiring-resolve (:create-pipeline config))
        render-pipeline (map requiring-resolve (:render-pipeline config))]
    (doseq [ns (:requires config)]
      #_(println "requiring " ns)
      (require ns))
    (cdq.application.desktop/application!
     config
     (proxy [ApplicationAdapter] []
       (create []
         (reset! state (reduce (fn [context f]
                                 (f context config))
                               {}
                               create-pipeline)))

       (dispose []
         (doseq [[k obj] @state]
           (if (instance? Disposable obj)
             (do
              #_(println "Disposing:" k)
              (Disposable/.dispose obj))
             #_(println "Not Disposable: " k ))))

       (render []
         (swap! state (fn [context]
                        (reduce (fn [context f]
                                  (f context))
                                context
                                render-pipeline))))

       (resize [width height]
         (let [context @state]
           (Viewport/.update (:cdq.graphics/ui-viewport    context) width height true)
           (Viewport/.update (:cdq.graphics/world-viewport context) width height false)))))))

(defn post-runnable!
  "`f` should be a `(fn [context])`.

  Is executed after the main-loop, in order not to interfere with it."
  [f]
  (.postRunnable Gdx/app (fn [] (f @state))))
