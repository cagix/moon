(ns cdq.ui.menu
  (:require [cdq.application]
            [clojure.walk :as walk])
  (:import (clojure.lang MultiFn)))

(def impls (walk/postwalk
            cdq.application/require-resolve-symbols
            '[[clojure.scene2d/build
               :actor.type/actor
               clojure.gdx.scene2d.actor/create]
              [clojure.scene2d/build
               :actor.type/group
               clojure.gdx.scene2d.group/create]
              [clojure.scene2d/build
               :actor.type/widget
               clojure.gdx.scene2d.actor/create-widget]
              [clojure.scene2d/build
               :actor.type/menu-bar
               clojure.gdx.scene2d.actor.menu-bar/create]
              [clojure.scene2d/build
               :actor.type/select-box
               clojure.vis-ui.select-box/create]
              [clojure.scene2d/build
               :actor.type/label
               clojure.vis-ui.label/create]
              [clojure.scene2d/build
               :actor.type/text-field
               clojure.vis-ui.text-field/create]
              [clojure.scene2d/build
               :actor.type/check-box
               clojure.vis-ui.check-box/create]
              [clojure.scene2d/build
               :actor.type/table
               clojure.vis-ui.table/create]
              [clojure.scene2d/build
               :actor.type/image-button
               clojure.vis-ui.image-button/create]
              [clojure.scene2d/build
               :actor.type/text-button
               clojure.vis-ui.text-button/create]
              [clojure.scene2d/build
               :actor.type/window
               clojure.vis-ui.window/create]
              [clojure.scene2d/build
               :actor.type/image
               clojure.vis-ui.image/create]]))

(defn init! [ctx]
  (doseq [[defmulti-var k method-fn-var] impls]
    (assert (var? defmulti-var))
    (assert (keyword? k))
    (assert (var? method-fn-var))
    (MultiFn/.addMethod @defmulti-var k method-fn-var))
  ctx)
