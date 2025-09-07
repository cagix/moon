(ns cdq.application.lwjgl
  (:require cdq.gdx-app.dispose
            cdq.gdx-app.resize
            [cdq.application :as application]
            [cdq.application.context.record :as ctx-record]
            [cdq.malli :as m]
            [clojure.gdx.backends.lwjgl :as lwjgl]))

(defn start!
  [{:keys [ctx/create-pipeline
           ctx/render-pipeline]}]
  (lwjgl/start-application!
   {:create! (fn []
               (reset! application/state (reduce (fn [ctx f]
                                                   (let [result (f ctx)]
                                                     (if (nil? result)
                                                       ctx
                                                       result)))
                                                 (ctx-record/map->Context {:schema (m/schema ctx-record/schema)})
                                                 (map requiring-resolve create-pipeline))))
    :dispose! (fn []
                (cdq.gdx-app.dispose/do! @application/state))
    :render! (fn []
               (swap! application/state (fn [ctx]
                                          (reduce (fn [ctx f]
                                                    (if-let [new-ctx (f ctx)]
                                                      new-ctx
                                                      ctx))
                                                  ctx
                                                  (map requiring-resolve render-pipeline)))))
    :resize! (fn [width height]
               (cdq.gdx-app.resize/do! @application/state width height))
    :pause! (fn [])
    :resume! (fn [])}
   {:title "Cyber Dungeon Quest"
    :windowed-mode {:width 1440 :height 900}
    :foreground-fps 60}))
