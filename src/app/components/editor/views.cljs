(ns app.components.editor.views
  (:require
   [app.components.editor.dnd :as dnd]
   [app.utils.css.core :refer-macros [css]]
   [cuerdas.core :as str]
   [goog.math :as gmath]
   [uix.core :as uix :refer [$ defui]]))

;; Styles ----------------------------------------------------------------------

(css wrapper-css []
  {:position "absolute"
   :inset 0
   :display "flex"
   :flex-direction "column"
   :align-items "center"
   :justify-content "center"
   :gap "10px"
   :background "oklch(10% 0.15 200)"
   "button" {:background "white"
             :color "black"
             :padding "var(--spacing-1-5) var(--spacing-2)"
             :border-radius "var(--spacing-1)"}})

(css video-css []
  {:display "block"
   :width "100%"
   :height "auto"
   :max-width "600px"})

(css video-wrapper-css []
  {:position "relative"})

(css cropper-css []
  {"--border-color" "oklch(0% 0 0 / 0.5)"
   :position "absolute"
   :inset 0
   :border "1px solid oklch(100% 0 0)"
   :box-shadow "inset 0 0 0 1px var(--border-color), 0 0 0 999px oklch(0% 0 0 / 0.25)"})

(css cropper-handle-css [k]
  {:position "absolute"
   :z-index "1"
   :background "oklch(100% 0 0)"
   :box-shadow "0 0 0 1px var(--border-color)"
   :width "var(--size)"
   :aspect-ratio "1 / 1"
   :border-radius "99999px"})

(css cropper-bar-css []
  {:position "absolute"})

(css cropper-wrapper-css []
  {"--size" "10px"
   "--offset" "calc(var(--size) / 2)"
   "--border-color" "oklch(0% 0 0 / 0.5)"
   :position "absolute"
   :z-index 1
   :overflow "hidden"
   :inset 0})

(css cropper-bar-border-css []
  {:position "absolute"
   :background "white"})

(css ffmpeg-command-css []
  {:background "white"
   :padding "10px"
   :width "100%"
   :font-family "monospace"
   :border-radius "5px"})

(css alignment-bar-css []
  {:position "absolute"
   :background "oklch(100% 0 0 / 0.3)"})

;; Component -------------------------------------------------------------------

(defn px [v]
  (str v "px"))

(defui CropCircle [{:keys [direction on-drag-move on-drag-end]}]
  (let [size-offset "calc(var(--offset) * -1)"
        center-offset "calc(50% - var(--offset))"
        {:keys [on-pointer-down]} (dnd/use-draggable {:on-drag-move on-drag-move
                                                      :on-drag-end on-drag-end
                                                      :meta {:directions #{direction}}})
        on-pointer-down (fn [e]
                          (.stopPropagation e)
                          (on-pointer-down e))]
    ($ :<>
       ($ :div {:class (cropper-bar-css)
                :style (case direction
                         :top {:top size-offset
                               :left 0
                               :right 0
                               :height "10px"
                               :cursor "row-resize"
                               :z-index 1}
                         :bottom {:bottom size-offset
                                  :left 0
                                  :right 0
                                  :height "10px"
                                  :cursor "row-resize"
                                  :z-index 1}
                         :right {:right size-offset
                                 :top 0
                                 :bottom 0
                                 :width "10px"
                                 :cursor "col-resize"
                                 :z-index 1}
                         :left {:left size-offset
                                :top 0
                                :bottom 0
                                :width "10px"
                                :cursor "col-resize"
                                :z-index 1})
                :on-pointer-down on-pointer-down})
       ($ :div
          {:style (case direction
                    :top    {:top size-offset
                             :left center-offset
                             :cursor "row-resize"}
                    :bottom {:bottom size-offset
                             :left center-offset
                             :cursor "row-resize"}
                    :right  {:top center-offset
                             :right size-offset
                             :cursor "col-resize"}
                    :left   {:left size-offset
                             :top center-offset
                             :cursor "col-resize"})
           :class (cropper-handle-css direction)
           :on-pointer-down on-pointer-down}))))

(defui AlignmentBars [{:keys []}]
  ($ :<>
     ($ :div {:class-name (alignment-bar-css)
              :style {:width "1px"
                      :top 0
                      :bottom 0
                      :left "33%"}})
     ($ :div {:class-name (alignment-bar-css)
              :style {:width "1px"
                      :top 0
                      :bottom 0
                      :right "33%"}})
     ($ :div {:class-name (alignment-bar-css)
              :style {:height "1px"
                      :left 0
                      :right 0
                      :top "33%"}})
     ($ :div {:class-name (alignment-bar-css)
              :style {:height "1px"
                      :left 0
                      :right 0
                      :bottom "33%"}})))

(defui CropRect [{:keys [ref offset children on-drag-move on-drag-end]}]
  (let [{:keys [on-pointer-down]} (dnd/use-draggable {:on-drag-move on-drag-move
                                                      :on-drag-end on-drag-end
                                                      :meta {:directions #{:top :right :bottom :left}}})]
    ($ :div {:ref ref
             :class (cropper-css)
             :style {:top (px (:top offset))
                     :bottom (px (:bottom offset))
                     :right (px (:right offset))
                     :left (px (:left offset))}
             :on-pointer-down on-pointer-down}
       ($ AlignmentBars)
       children)))

(defui Cropper [{:keys [resizer-ref offset video-dimensions on-drag-move on-drag-end]}]
  ($ :div {:class (cropper-wrapper-css)}
     ($ CropRect {:ref resizer-ref
                  :offset offset
                  :video-dimensions video-dimensions
                  :on-drag-move on-drag-move
                  :on-drag-end on-drag-end}
        (for [direction [:top :right :bottom :left]]
          ($ CropCircle {:key direction
                         :direction direction
                         :offset offset
                         :on-drag-move on-drag-move
                         :on-drag-end on-drag-end})))))

(defn ffmpeg-command [{:keys [offset file-name video-dimensions]}]
  (let [{:keys [element-width width element-height height]} video-dimensions
        width-ratio (/ width element-width)
        height-ratio (/ height element-height)
        x (* (:left offset) width-ratio)
        y (* (:top offset) height-ratio)
        dst-width (* (- element-width (:left offset) (:right offset)) width-ratio)
        dst-height (* (- element-height (:top offset) (:bottom offset)) height-ratio)]
    (str/join " " ["ffmpeg"
                   "-i" (str "file:" file-name)
                   "-vf" (str "\"crop=" (->> [dst-width dst-height x y]
                                             (map js/Math.round)
                                             (str/join ":")) "\"")
                   "-y" "output.mp4"])))

(defn clamp
  "Takes a number and clamps it to within the provided bounds.
  Returns the input number if it is within bounds, or the nearest number within
  the bounds."
  [value min max]
  (gmath/clamp value min max))

(defui Timeline [{:keys [video-ref]}]
  (let [[playing? set-playing?!] (uix/use-state false)]
    (uix/use-effect
     (fn [] ()
       (let [set-pause! (comp set-playing?! not)]
         (.addEventListener @video-ref "pause" set-pause!)
         (.addEventListener @video-ref "playing" set-playing?!)
         (fn []
           (when @video-ref
             (.removeEventListener @video-ref "pause" set-pause!)
             (.removeEventListener @video-ref "playing" set-playing?!)))))
     [video-ref])
    ($ :div
       ($ :button
          {:on-click (fn []
                       (if (.-paused @video-ref)
                         (.play @video-ref)
                         (.pause @video-ref)))}
          (if playing? "Pause" "Play")))))

(defui Editor []
  (let [[file-name set-file-name!] (uix/use-state nil)
        [video-dimensions set-video-dimensions!] (uix/use-state nil)
        default-offset {:top 0
                        :bottom 0
                        :left 0
                        :right 0}
        [offset set-offset!] (uix/use-state default-offset)
        max-height (:element-height video-dimensions)
        max-width (:element-width video-dimensions)
        padding 10
        top-offset (fn [y]
                     (-> (+ y (:top offset))
                         (gmath/clamp 0 (- max-height (:bottom offset) padding))))
        bottom-offset (fn [y]
                        (-> (+ y (:bottom offset))
                            (gmath/clamp 0 (- max-height (:top offset) padding))))
        right-offset (fn [x]
                       (-> (+ x (:right offset))
                           (gmath/clamp 0 (- max-width (:left offset) padding))))
        left-offset (fn [x]
                      (-> (+ x (:left offset))
                          (gmath/clamp 0 (- max-width (:right offset) padding))))
        [video-url set-video-url!] (uix/use-state nil)
        load-video! (uix/use-callback
                     (fn [url]
                       (set-offset! default-offset)
                       (set-video-url! url))
                     [default-offset])
        example-video-url "http://commondatastorage.googleapis.com/gtv-videos-bucket/sample/BigBuckBunny.mp4"
        resizer-ref (uix/use-ref)
        video-ref (uix/use-ref)]
    (uix/use-effect
     (fn [] ()
       (when (and @video-ref video-url)
         (let [f (fn [_e]
                   (when (= video-url example-video-url)
                     (set! (.-currentTime @video-ref) 10))
                   (set-video-dimensions! {:width (.-videoWidth @video-ref)
                                           :height (.-videoHeight @video-ref)
                                           :element-width (.-clientWidth @video-ref)
                                           :element-height (.-clientHeight @video-ref)}))]
           (.addEventListener @video-ref "loadedmetadata" f {:once true})
           #(when @video-ref (.removeEventListener @video-ref "loadedmetadata" f)))))
     [video-url example-video-url])
    ($ :div {:class (wrapper-css)}
       ($ :button
          {:on-pointer-down #(load-video! example-video-url)}
          "Load example")
       ($ :button
          {:on-pointer-down #(set-offset! default-offset)}
          "Reset")
       ($ :input
          {:type "file"
           :accept "video/*"
           :on-change (fn [e]
                        (let [file (-> e .-target .-files (aget 0))
                              url (js/URL.createObjectURL file)]
                          (set-file-name! (.-name file))
                          (load-video! url)))})
       (when video-url
         ($ :<>
            ($ :div {:class (video-wrapper-css)}
               ($ Cropper {:resizer-ref resizer-ref
                           :offset offset
                           :video-dimensions video-dimensions
                           :on-drag-move (fn [{:keys [meta delta] :as _opts}]
                                           (let [{:keys [directions]} meta
                                                 {:keys [x y]} delta
                                                 style (.. @resizer-ref -style)]
                                             (when (:top directions)
                                               (set! (.. style -top) (px (top-offset (- y)))))
                                             (when (:bottom directions)
                                               (set! (.. style -bottom) (px (bottom-offset y))))
                                             (when (:left directions)
                                               (set! (.. style -left) (px (left-offset (- x)))))
                                             (when (:right directions)
                                               (set! (.. style -right) (px (right-offset x))))))
                           :on-drag-end (fn [{:keys [meta delta] :as _opts}]
                                          (let [{:keys [directions]} meta
                                                {:keys [x y]} delta
                                                new-offset (cond-> offset
                                                             (:top directions) (assoc :top (top-offset (- y)))
                                                             (:bottom directions) (assoc :bottom (bottom-offset y))
                                                             (:left directions) (assoc :left (left-offset (- x)))
                                                             (:right directions) (assoc :right (right-offset x)))]
                                            (set-offset! new-offset)))})
               ($ :video
                  {:class [(video-css)]
                   :ref video-ref
                   :src video-url}))
            ($ Timeline
               {:video-ref video-ref}
               (let [command (ffmpeg-command {:offset offset
                                              :file-name file-name
                                              :video-dimensions video-dimensions})]
                 ($ :input {:class (ffmpeg-command-css)
                            :read-only true
                            #_#_:style {:width (str (count command) "ch")}
                            :value command
                            :on-click #(.select (.-target %))}))))))))
