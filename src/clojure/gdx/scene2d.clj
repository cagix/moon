(ns clojure.gdx.scene2d
  (:require [cdq.ctx.graphics :as graphics]
            [clojure.scene2d :as scene2d]
            [clojure.gdx.scene2d.actor :as actor]
            [clojure.gdx.scene2d.group :as group]
            [clojure.gdx.scene2d.ctx-stage :as ctx-stage])
  (:import (com.badlogic.gdx.scenes.scene2d Group)))

(defn- try-act [actor delta f]
  (when-let [ctx (when-let [stage (actor/get-stage actor)]
                   (ctx-stage/get-ctx stage))]
    (f actor delta ctx)))

(defn try-draw [actor f]
  (when-let [ctx (when-let [stage (actor/get-stage actor)]
                   (ctx-stage/get-ctx stage))]
    (graphics/handle-draws! (:ctx/graphics ctx)
                            (f actor ctx))))

(defn- actor
  [{:keys [act draw]
    :as opts}]
  (actor/create
   (assoc opts
          :actor/act (fn [actor delta]
                       (when act
                         (try-act actor delta act)))
          :actor/draw (fn [actor _batch _parent-alpha]
                        (when draw
                          (try-draw actor draw))))))

(defmethod scene2d/build :actor.type/actor [opts] (actor opts))

(defn set-group-opts! [group opts]
  (run! (fn [actor-or-decl]
          ; remove if
          ; inventory widget/image
          ; inventory itself widget/window
          ; cdq.ui.windows entity info window
          (group/add! group (if (instance? com.badlogic.gdx.scenes.scene2d.Actor actor-or-decl)
                              actor-or-decl
                              (scene2d/build actor-or-decl))))
        (:group/actors opts))
  (actor/set-opts! group opts))

(defn- group [opts]
  (doto (Group.)
    (set-group-opts! opts)))

(defmethod scene2d/build :actor.type/group [opts] (group opts))
