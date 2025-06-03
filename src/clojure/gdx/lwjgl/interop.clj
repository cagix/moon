; This is the first step - interop - it is plain 'calculations'
; and will never change
; also use namespaced keywords as of what it is .... 'clojure.gdx.graphics.monitor/foo'
; or gl-emulation/foobar
; => helps with the 'no private' mantra -> re-use at 'clojure.gdx.i'
; make everything public then you know what you have.
(ns clojure.gdx.lwjgl.interop
  (:import (com.badlogic.gdx.backends.lwjgl3 Lwjgl3Application$GLDebugMessageSeverity
                                             Lwjgl3ApplicationConfiguration$GLEmulation
                                             Lwjgl3Graphics$Lwjgl3DisplayMode
                                             Lwjgl3Graphics$Lwjgl3Monitor)))

(defn k->gl-debug-message-severity [k]
  (case k
    :high         Lwjgl3Application$GLDebugMessageSeverity/HIGH
    :medium       Lwjgl3Application$GLDebugMessageSeverity/MEDIUM
    :low          Lwjgl3Application$GLDebugMessageSeverity/LOW
    :notification Lwjgl3Application$GLDebugMessageSeverity/NOTIFICATION))

(defn k->glversion [gl-version]
  (case gl-version
    :angle-gles20 Lwjgl3ApplicationConfiguration$GLEmulation/ANGLE_GLES20
    :gl20         Lwjgl3ApplicationConfiguration$GLEmulation/GL20
    :gl30         Lwjgl3ApplicationConfiguration$GLEmulation/GL30
    :gl31         Lwjgl3ApplicationConfiguration$GLEmulation/GL31
    :gl32         Lwjgl3ApplicationConfiguration$GLEmulation/GL32))

(defn display-mode->map [^Lwjgl3Graphics$Lwjgl3DisplayMode display-mode]
  {:width          (.width        display-mode)
   :height         (.height       display-mode)
   :refresh-rate   (.refreshRate  display-mode)
   :bits-per-pixel (.bitsPerPixel display-mode)
   :monitor-handle (.getMonitor   display-mode)})

(defn monitor->map [^Lwjgl3Graphics$Lwjgl3Monitor monitor]
  {:virtual-x      (.virtualX         monitor)
   :virtual-y      (.virtualY         monitor)
   :name           (.name             monitor)
   :monitor-handle (.getMonitorHandle monitor)})

(defn map->display-mode [{:keys [width height refresh-rate bits-per-pixel monitor-handle]}]
  (let [constructor (.getDeclaredConstructor Lwjgl3Graphics$Lwjgl3DisplayMode
                                             (into-array Class [Long/TYPE Integer/TYPE Integer/TYPE Integer/TYPE Integer/TYPE]))
        _ (.setAccessible constructor true)]
    (.newInstance constructor
                  (into-array Object [monitor-handle width height refresh-rate bits-per-pixel]))))

(defn map->monitor [{:keys [virtual-x virtual-y name monitor-handle]}]
  (let [constructor (.getDeclaredConstructor Lwjgl3Graphics$Lwjgl3Monitor
                                             (into-array Class [Long/TYPE Integer/TYPE Integer/TYPE String]))
        _ (.setAccessible constructor true)]
    (.newInstance constructor
                  (into-array Object [monitor-handle virtual-x virtual-y name]))))
