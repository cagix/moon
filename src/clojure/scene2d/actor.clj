(ns clojure.scene2d.actor
  (:require [com.badlogic.gdx.scenes.scene2d.actor :as actor]
            [com.badlogic.gdx.scenes.scene2d.ctx :as ctx]
            [com.badlogic.gdx.scenes.scene2d.stage :as stage]))

(def opts-fn-map
  {:actor/name actor/set-name!
   :actor/user-object actor/set-user-object!
   :actor/visible?  actor/set-visible!
   :actor/touchable actor/set-touchable!
   :actor/listener actor/add-listener!
   :actor/position (fn [actor [x y]]
                     (actor/set-position! actor x y))
   :actor/center-position (fn [actor [x y]]
                            (actor/set-position! actor
                                           (- x (/ (actor/get-width  actor) 2))
                                           (- y (/ (actor/get-height actor) 2))))})

(defn set-opts! [actor opts]
  (doseq [[k v] opts
          :let [f (get opts-fn-map k)]
          :when f]
    (f actor v))
  actor)

(defn- get-ctx [actor]
  (when-let [stage (actor/get-stage actor)]
    (stage/get-ctx stage)))

(defn act! [actor delta f]
  (when-let [ctx (get-ctx actor)]
    (f actor delta ctx)))

(defn draw! [actor f]
  (when-let [ctx (get-ctx actor)]
    (ctx/draw! ctx (f actor ctx))))
