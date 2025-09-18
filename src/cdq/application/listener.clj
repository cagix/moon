(ns cdq.application.listener
  (:require [cdq.application :as application]))

(defn create
  [{:keys [create
           dispose
           render
           resize]}]
  {:create! (fn [gdx]
              (let [[f pipeline] create]
                (f application/state gdx pipeline)))
   :dispose! (fn []
               (dispose application/state))
   :render! (fn []
              (let [[f pipeline] render]
                (f application/state pipeline)))
   :resize! (fn [width height]
              (resize application/state width height))
   :pause! (fn [])
   :resume! (fn [])})
