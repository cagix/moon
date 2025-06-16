(ns cdq.application
  (:require [cdq.malli :as m]
            [cdq.utils :as utils]
            [gdl.graphics :as graphics]
            [gdl.utils.disposable :as disp]
            [qrecord.core :as q]))

(defn initial-context [ks]
  (in-ns 'cdq.application) ; otherwise 'user
  (eval `(q/defrecord ~'Context ~(mapv (comp symbol first) ks)))
  (def schema (m/schema (apply vector :map {:closed true} ks)))
  (eval `(map->Context {})))

(defn validate-ctx [ctx]
  (m/validate-humanize schema ctx)
  ctx)

(defn invoke [[f params]]
  (f params))

(def state (atom nil))

(defn create! [{:keys [initial-context create-fns]}]
  (reset! state (reduce utils/render*
                        (invoke initial-context)
                        create-fns)))

(defn dispose! []
  (let [{:keys [ctx/audio
                ctx/graphics
                ctx/tiled-map]} @state]
    (disp/dispose! audio)
    (disp/dispose! graphics)
    (disp/dispose! tiled-map)
    ; TODO vis-ui dispose
    ; TODO what else disposable?
    ; => :ctx/tiled-map definitely and also dispose when re-creting gamestate.
    ))

(defn render! [render-fns]
  (swap! state (fn [ctx]
                 (reduce utils/render* ctx render-fns))))

(defn resize! [width height]
  (let [{:keys [ctx/graphics]} @state]
    (graphics/resize-viewports! graphics width height)))
