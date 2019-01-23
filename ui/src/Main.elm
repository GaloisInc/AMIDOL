module Main exposing (main)

import Browser
import Draggable
import Draggable.Events exposing (onClick, onDragBy, onDragStart)
import Html exposing (Html)
import Html.Attributes
import Math.Vector2 as Vector2 exposing (Vec2, getX, getY)
import Svg exposing (Svg)
import Svg.Attributes as Attr
import Svg.Events exposing (onMouseUp)
import Svg.Keyed
import Svg.Lazy exposing (lazy)


main : Program () Model Msg
main =
    Browser.element
        { init = init
        , update = update
        , subscriptions = subscriptions
        , view = view
        }


type alias Id =
    String


type alias FileName =
    String


type alias Node =
    { id : Id
    , position : Vec2
    , clicked : Bool
    , image : FileName
    , label : String
    }


makeNode : Id -> Vec2 -> String -> String -> Node
makeNode id position image label =
    Node id position False image label


dragNodeBy : Vec2 -> Node -> Node
dragNodeBy delta node =
    { node | position = node.position |> Vector2.add delta }


toggleClicked : Node -> Node
toggleClicked node =
    { node | clicked = not node.clicked }


type alias NodeGroup =
    { uid : Int
    , movingNode : Maybe Node
    , idleNodes : List Node
    }


emptyGroup : NodeGroup
emptyGroup =
    NodeGroup 0 Nothing []


addNode : ( Vec2, FileName ) -> NodeGroup -> NodeGroup
addNode ( position, filename ) ({ uid, idleNodes } as group) =
    { group
        | idleNodes = makeNode (String.fromInt uid) position ("img/" ++ filename) "" :: idleNodes
        , uid = uid + 1
    }


makeNodeGroup : List ( Vec2, FileName ) -> NodeGroup
makeNodeGroup nodes =
    nodes
        |> List.foldl addNode emptyGroup


allNodes : NodeGroup -> List Node
allNodes { movingNode, idleNodes } =
    movingNode
        |> Maybe.map (\a -> a :: idleNodes)
        |> Maybe.withDefault idleNodes


startDragging : Id -> NodeGroup -> NodeGroup
startDragging id ({ idleNodes, movingNode } as group) =
    let
        ( targetAsList, others ) =
            List.partition (.id >> (==) id) idleNodes
    in
    { group
        | idleNodes = others
        , movingNode = targetAsList |> List.head
    }


stopDragging : NodeGroup -> NodeGroup
stopDragging group =
    { group
        | idleNodes = allNodes group
        , movingNode = Nothing
    }


dragActiveBy : Vec2 -> NodeGroup -> NodeGroup
dragActiveBy delta group =
    { group | movingNode = group.movingNode |> Maybe.map (dragNodeBy delta) }


toggleNodeClicked : Id -> NodeGroup -> NodeGroup
toggleNodeClicked id group =
    let
        possiblyToggleNode node =
            if node.id == id then
                toggleClicked node

            else
                node
    in
    { group | idleNodes = group.idleNodes |> List.map possiblyToggleNode }


type alias Model =
    { nodeGroup : NodeGroup
    , drag : Draggable.State Id
    }


type Msg
    = DragMsg (Draggable.Msg Id)
    | OnDragBy Vec2
    | StartDragging String
    | ToggleNodeClicked String
    | StopDragging


initialNodes : List ( Vec2, FileName )
initialNodes =
    [ ( Vector2.vec2 10 10, "susceptible.svg" )
    , ( Vector2.vec2 10 70, "infected.svg" )
    , ( Vector2.vec2 10 130, "recovered.svg" )
    ]


init : flags -> ( Model, Cmd Msg )
init _ =
    ( { nodeGroup = makeNodeGroup initialNodes
      , drag = Draggable.init
      }
    , Cmd.none
    )


dragConfig : Draggable.Config Id Msg
dragConfig =
    Draggable.customConfig
        [ onDragBy (\( dx, dy ) -> Vector2.vec2 dx dy |> OnDragBy)
        , onDragStart StartDragging
        , onClick ToggleNodeClicked
        ]



-- UPDATE


update : Msg -> Model -> ( Model, Cmd Msg )
update msg ({ nodeGroup } as model) =
    case msg of
        OnDragBy delta ->
            ( { model | nodeGroup = nodeGroup |> dragActiveBy delta }, Cmd.none )

        StartDragging id ->
            ( { model | nodeGroup = nodeGroup |> startDragging id }, Cmd.none )

        StopDragging ->
            ( { model | nodeGroup = nodeGroup |> stopDragging }, Cmd.none )

        ToggleNodeClicked id ->
            ( { model | nodeGroup = nodeGroup |> toggleNodeClicked id }, Cmd.none )

        DragMsg dragMsg ->
            Draggable.update dragConfig dragMsg model


subscriptions : Model -> Sub Msg
subscriptions { drag } =
    Draggable.subscriptions DragMsg drag



-- VIEW


nodeSize : Vec2
nodeSize =
    Vector2.vec2 50 50


view : Model -> Html Msg
view { nodeGroup } =
    Html.div
        []
        [ Html.p
            [ Html.Attributes.style "padding-left" "8px" ]
            [ Html.text "Drag any icon around." ]
        , Svg.svg
            [ Attr.style "height: 100vh; width: 100vw; position: fixed;"
            ]
            [ background
            , nodesView nodeGroup
            ]
        ]


nodesView : NodeGroup -> Svg Msg
nodesView nodeGroup =
    nodeGroup
        |> allNodes
        --|> List.reverse
        |> List.map nodeView
        |> Svg.node "g" []


nodeView : Node -> Svg Msg
nodeView { id, position, clicked, image, label } =
    Svg.image
        [ Attr.xlinkHref image
        , num Attr.width <| getX nodeSize
        , num Attr.height <| getY nodeSize
        , num Attr.x (getX position)
        , num Attr.y (getY position)
        , Attr.cursor "move"
        , Draggable.mouseTrigger id DragMsg
        , onMouseUp StopDragging
        ]
        []


background : Svg msg
background =
    Svg.rect
        [ Attr.x "0"
        , Attr.y "0"
        , Attr.width "100%"
        , Attr.height "100%"
        , Attr.fill "#eee"
        ]
        []


num : (String -> Svg.Attribute msg) -> Float -> Svg.Attribute msg
num attr value =
    attr (String.fromFloat value)
