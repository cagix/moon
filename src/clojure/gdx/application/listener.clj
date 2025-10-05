(ns clojure.gdx.application.listener
  "Clojure API for `com.badlogic.gdx.ApplicationListener`

  An `ApplicationListener` is called when an `com.badlogic.gdx.Application` is created, resumed, rendering, paused or destroyed. All methods are called in a thread that has the OpenGL context current. You can thus safely create and manipulate graphics resources.

The ApplicationListener interface follows the standard Android activity life-cycle and is emulated on the desktop accordingly."
  (:import (com.badlogic.gdx ApplicationListener)))

(defn create
  "Reifies an instance of `ApplicationListener` interface via the supplied function map.

  | Key | Function | Description |
  | ---- | ---- | ---- |
  | `:create` | `(fn [])` | Called when the `Application` is first created. |
  | `:dispose` | `(fn [])` | Called when the `Application` is destroyed. Preceded by a call to `pause`. |
  | `:render` | `(fn [])` | Called when the `Application` should render itself. |
  | `:resize` | `(fn [width height])` | Called when the `Application` is resized. This can happen at any point during a non-paused state but will never happen before a call to `create`. |
  | `:pause` | `(fn [])` | Called when the `Application` is paused, usually when it's not active or visible on-screen. An Application is also paused before it is destroyed. |
  | `:resume` | `(fn [])` | Called when the `Application` is resumed from a paused state, usually when it regains focus.|"
  [{:keys [create dispose render resize pause resume]}]
  (reify ApplicationListener
    (create [_]
      (create))
    (dispose [_]
      (dispose))
    (render [_]
      (render))
    (resize [_ width height]
      (resize width height))
    (pause [_]
      (pause))
    (resume [_]
      (resume))))
